package com.github.ilpersi.BHBot;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public class DiscordManager {
    private final BHBot bot;

    DiscordManager(BHBot bot) {
        this.bot = bot;
    }

    synchronized void sendDiscordMessage(String content, File attachment) {
        if (bot.settings.enableDiscord) {

            if ("https://discordapp.com/api/webhooks/your_hook/your_token".equals(bot.settings.discordWebHookUrl)) {
                BHBot.logger.warn("Discord webhook not correctly configured! Disabling Discord notifications");
                bot.settings.enableDiscord = false;
                return;
            }

            if (!"".equals(bot.settings.username) && !"yourusername".equals(bot.settings.username)) {
                content = "[" + bot.settings.username + "]\n" + content;
            }

            DiscordWebHookMessage discordWebHookMessage = new DiscordWebHookMessage();
            discordWebHookMessage.userName = bot.settings.discordUserName;
            discordWebHookMessage.messageText = content;

            Gson gson = new Gson();

            MultiPartBodyPublisher publisher = new MultiPartBodyPublisher()
                    .addPart("payload_json", gson.toJson(discordWebHookMessage));

            if (attachment != null) {
                publisher.addPart("file", () -> {
                    try {
                        return new FileInputStream(attachment);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return new ByteArrayInputStream("".getBytes());
                }, attachment.getName(), "image/png");
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bot.settings.discordWebHookUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + publisher.getBoundary())
                    .timeout(Duration.ofMinutes(1))
                    .POST(publisher.build())
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    BHBot.logger.warn("Invalid HTTP Status when sending Discord notification!");
                }

            } catch (IOException | InterruptedException e) {
                BHBot.logger.error("Exception while getting latest version info from Git Hub", e);
            }
        }
    }
}

class DiscordWebHookMessage {

    @SerializedName(value = "username")
    String userName;

    @SerializedName(value = "content")
    String messageText;

}

// https://stackoverflow.com/questions/46392160/java-9-httpclient-send-a-multipart-form-data-request
class MultiPartBodyPublisher {
    private final List<PartsSpecification> partsSpecificationList = new ArrayList<>();
    private final String boundary = UUID.randomUUID().toString();

    public HttpRequest.BodyPublisher build() {
        if (partsSpecificationList.size() == 0) {
            throw new IllegalStateException("Must have at least one part to build multipart message.");
        }
        addFinalBoundaryPart();
        return HttpRequest.BodyPublishers.ofByteArrays(PartsIterator::new);
    }

    String getBoundary() {
        return boundary;
    }

    MultiPartBodyPublisher addPart(@SuppressWarnings("SameParameterValue") String name, String value) {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.STRING;
        newPart.name = name;
        newPart.value = value;
        partsSpecificationList.add(newPart);
        return this;
    }

    /*MultiPartBodyPublisher addPart(String name, Path value) {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.FILE;
        newPart.name = name;
        newPart.path = value;
        partsSpecificationList.add(newPart);
        return this;
    }*/

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    MultiPartBodyPublisher addPart(String name, Supplier<InputStream> value, String filename, String contentType) {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.STREAM;
        newPart.name = name;
        newPart.stream = value;
        newPart.filename = filename;
        newPart.contentType = contentType;
        partsSpecificationList.add(newPart);
        return this;
    }

    private void addFinalBoundaryPart() {
        PartsSpecification newPart = new PartsSpecification();
        newPart.type = PartsSpecification.TYPE.FINAL_BOUNDARY;
        newPart.value = "--" + boundary + "--";
        partsSpecificationList.add(newPart);
    }

    static class PartsSpecification {

        public enum TYPE {
            STRING, FILE, STREAM, FINAL_BOUNDARY
        }

        PartsSpecification.TYPE type;
        String name;
        String value;
        Path path;
        Supplier<InputStream> stream;
        String filename;
        String contentType;

    }

    class PartsIterator implements Iterator<byte[]> {

        private final Iterator<PartsSpecification> iter;
        private InputStream currentFileInput;

        private boolean done;
        private byte[] next;

        PartsIterator() {
            iter = partsSpecificationList.iterator();
        }

        @Override
        public boolean hasNext() {
            if (done) return false;
            if (next != null) return true;
            try {
                next = computeNext();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (next == null) {
                done = true;
                return false;
            }
            return true;
        }

        @Override
        public byte[] next() {
            if (!hasNext()) throw new NoSuchElementException();
            byte[] res = next;
            next = null;
            return res;
        }

        private byte[] computeNext() throws IOException {
            if (currentFileInput == null) {
                if (!iter.hasNext()) return null;
                PartsSpecification nextPart = iter.next();
                if (PartsSpecification.TYPE.STRING.equals(nextPart.type)) {
                    String part =
                            "--" + boundary + "\r\n" +
                                    "Content-Disposition: form-data; name=" + nextPart.name + "\r\n" +
                                    "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                                    nextPart.value + "\r\n";
                    return part.getBytes(StandardCharsets.UTF_8);
                }
                if (PartsSpecification.TYPE.FINAL_BOUNDARY.equals(nextPart.type)) {
                    return nextPart.value.getBytes(StandardCharsets.UTF_8);
                }
                String filename;
                String contentType;
                if (PartsSpecification.TYPE.FILE.equals(nextPart.type)) {
                    Path path = nextPart.path;
                    filename = path.getFileName().toString();
                    contentType = Files.probeContentType(path);
                    if (contentType == null) contentType = "application/octet-stream";
                    currentFileInput = Files.newInputStream(path);
                } else {
                    filename = nextPart.filename;
                    contentType = nextPart.contentType;
                    if (contentType == null) contentType = "application/octet-stream";
                    currentFileInput = nextPart.stream.get();
                }
                String partHeader =
                        "--" + boundary + "\r\n" +
                                "Content-Disposition: form-data; name=" + nextPart.name + "; filename=" + filename + "\r\n" +
                                "Content-Type: " + contentType + "\r\n\r\n";
                return partHeader.getBytes(StandardCharsets.UTF_8);
            } else {
                byte[] buf = new byte[8192];
                int r = currentFileInput.read(buf);
                if (r > 0) {
                    byte[] actualBytes = new byte[r];
                    System.arraycopy(buf, 0, actualBytes, 0, r);
                    return actualBytes;
                } else {
                    currentFileInput.close();
                    currentFileInput = null;
                    return "\r\n".getBytes(StandardCharsets.UTF_8);
                }
            }
        }
    }
}