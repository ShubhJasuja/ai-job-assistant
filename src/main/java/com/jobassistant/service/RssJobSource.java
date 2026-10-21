package com.jobassistant.service;

import com.jobassistant.entity.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RssJobSource implements JobSource {
    private static final Logger logger = LoggerFactory.getLogger(RssJobSource.class);
    private static final Pattern COMPANY_SUFFIX = Pattern.compile("(?i)\\s+(?:at|@|[-|])\\s+(.+)$");
    private static final Pattern LOCATION = Pattern.compile("(?im)^\\s*location\\s*:\\s*(.+)$");

    private final JobSourceProperties properties;
    private final JobSkillNormalizer skills;
    private final RestClient client = JobSourceHttpClient.create();

    public RssJobSource(JobSourceProperties properties, JobSkillNormalizer skills) {
        this.properties = properties;
        this.skills = skills;
    }

    @Override
    public String getSourceName() { return "rss"; }

    @Override
    public List<Job> fetch() {
        List<Job> jobs = new ArrayList<>();
        int successfulFeeds = 0;
        int failedFeeds = 0;
        for (String configuredFeed : properties.getRssFeeds()) {
            String[] feedConfig = configuredFeed.split("\\|", 2);
            String feedUrl = feedConfig[0].trim();
            String configuredCompany = feedConfig.length == 2 ? feedConfig[1].trim() : "";
            try {
                URI uri = validatePublicHttpsUri(feedUrl);
                String xml = JobSourceHttpClient.get(client, uri);
                if (xml == null || xml.isBlank()) {
                    successfulFeeds++;
                    continue;
                }
                for (Element item : feedItems(parseXml(xml))) {
                    Job job = toJob(item, feedUrl, configuredCompany);
                    if (job != null) jobs.add(job);
                }
                successfulFeeds++;
            } catch (Exception exception) {
                failedFeeds++;
                logger.warn("RSS feed could not be imported ({}) ({})",
                        safeHost(feedUrl), failureType(exception));
            }
        }
        if (!properties.getRssFeeds().isEmpty() && successfulFeeds == 0 && failedFeeds > 0) {
            throw new IllegalStateException("No configured RSS feed could be fetched");
        }
        return jobs;
    }

    private URI validatePublicHttpsUri(String value) throws Exception {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("RSS feeds must use an HTTPS URL with a public hostname");
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                || host.endsWith(".internal") || host.equals("metadata.google.internal")) {
            throw new IllegalArgumentException("RSS feed host is not public");
        }
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (isNonPublicAddress(address)) {
                throw new IllegalArgumentException("RSS feed host resolves to a non-public address");
            }
        }
        return uri;
    }

    private boolean isNonPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first == 0 || first == 10 || first == 127
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168);
        }
        int first = Byte.toUnsignedInt(bytes[0]);
        return (first & 0xfe) == 0xfc || (first == 0xfe && (Byte.toUnsignedInt(bytes[1]) & 0xc0) == 0x80);
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private List<Element> feedItems(Document document) {
        NodeList items = document.getElementsByTagNameNS("*", "item");
        if (items.getLength() == 0) items = document.getElementsByTagNameNS("*", "entry");
        List<Element> result = new ArrayList<>();
        for (int index = 0; index < items.getLength(); index++) {
            if (items.item(index) instanceof Element element) result.add(element);
        }
        return result;
    }

    private Job toJob(Element item, String feedUrl, String configuredCompany) {
        String title = childText(item, "title");
        if (title.isBlank()) return null;
        String link = childText(item, "link");
        if (link.isBlank()) link = childAttribute(item, "link", "href");
        String description = JobText.cleanHtml(firstNonBlank(
                childText(item, "description"),
                childText(item, "summary"),
                childText(item, "content")));
        String company = configuredCompany;
        if (company.isBlank()) {
            Matcher companyMatcher = COMPANY_SUFFIX.matcher(title);
            if (companyMatcher.find()) {
                company = companyMatcher.group(1).trim();
                title = title.substring(0, companyMatcher.start()).trim();
            } else {
                company = "RSS listing";
            }
        }
        String location = firstNonBlank(childText(item, "location"), capture(LOCATION, description));
        String sourceId = firstNonBlank(childText(item, "guid"), childText(item, "id"), link);
        if (sourceId.isBlank()) sourceId = title + "|" + company + "|" + childText(item, "pubDate");
        String posted = firstNonBlank(childText(item, "pubDate"), childText(item, "published"), childText(item, "updated"));
        String updated = childText(item, "updated");

        Job job = Job.builder()
                .title(title.trim())
                .company(company)
                .location(location)
                .remoteStatus(JobText.remoteStatus(location + " " + description))
                .employmentType(JobText.employmentType(title, description))
                .description(description)
                .applicationUrl(link)
                .source(getSourceName())
                .sourceJobId(safeId(feedUrl) + ":" + safeId(sourceId))
                .postedDate(parseDate(posted))
                .sourceUpdatedAt(parseTimestamp(updated))
                .build();
        skills.apply(job, List.of(), title, description);
        return job;
    }

    private String childText(Element parent, String localName) {
        NodeList elements = parent.getElementsByTagNameNS("*", localName);
        if (elements.getLength() == 0) elements = parent.getElementsByTagName(localName);
        if (elements.getLength() == 0) return "";
        Node node = elements.item(0);
        return node.getTextContent() == null ? "" : node.getTextContent().trim();
    }

    private String childAttribute(Element parent, String localName, String attribute) {
        NodeList elements = parent.getElementsByTagNameNS("*", localName);
        for (int index = 0; index < elements.getLength(); index++) {
            Node node = elements.item(index);
            if (node instanceof Element element && element.hasAttribute(attribute)) {
                return element.getAttribute(attribute).trim();
            }
        }
        return "";
    }

    private String capture(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) return null;
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate();
        } catch (RuntimeException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDate();
            } catch (RuntimeException ignoredAgain) {
                try {
                    return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDate();
                } catch (RuntimeException ignoredFinally) {
                    return null;
                }
            }
        }
    }

    private java.time.LocalDateTime parseTimestamp(String value) {
        if (value.isBlank()) return null;
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
        } catch (RuntimeException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant()
                        .atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            } catch (RuntimeException ignoredAgain) {
                try {
                    return Instant.parse(value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
                } catch (RuntimeException ignoredFinally) {
                    return null;
                }
            }
        }
    }

    private String failureType(Exception exception) {
        if (exception instanceof org.springframework.web.client.RestClientResponseException responseException) {
            return "HTTP " + responseException.getStatusCode().value();
        }
        return exception.getClass().getSimpleName();
    }

    private String safeId(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String safeHost(String value) {
        try {
            return URI.create(value).getHost();
        } catch (RuntimeException ignored) {
            return "invalid-url";
        }
    }
}
