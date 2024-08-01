import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class CrptApi {
    private final Semaphore semaphore;
    private final TimeUnit timeUnit;
    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (requestLimit <= 0) throw new IllegalArgumentException("Значение requestLimit должно быть больше нуля.");
        semaphore = new Semaphore(requestLimit);
        this.timeUnit = timeUnit;
    }

    public void createDocument(final Document document, final String signature) {
        if (document == null) throw new NullPointerException("Документ не сформирован для отправки.");
        if (signature == null) throw new NullPointerException("Документ не может быть без подписи");
        try {
            //данная часть кода будет выполняться заданным в конструктуре числом потоков
            semaphore.acquire();
            //создаем новый поток с задержкой времени указанной в конструктуре,
            //то есть получаем выполнение данного участка кода заданным количеством потоков за заданную единицу времени
            ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1);
            ses.schedule(() -> {
                //отображаем в консоли в формате (часы:минуты:секунды:милисекунды) строки чтобы видеть частоту
                //вызова метода и его корректную работу по синхронизации.
                System.out.println(DateTimeFormatter.ofPattern("HH:mm:ss:SS")
                        .format(LocalDateTime.now()));
                //Осуществялем запрос на создание документа в однопоточном методе
                try {
                    HttpResponse<String> httpResponse = sendRequest(document, signature);
                    //Анализ ответа
                    //analysisResponse(httpResponse);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    ses.shutdown();
                    semaphore.release(); //даем разрешение на новый вызов метода из очереди потоков
                }
            }, timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized HttpResponse<String> sendRequest(final Document document, final String signature)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(Objects.requireNonNull(UtilParseJson.DocumentToJsonString(document))))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void analysisResponse(final HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() == 200 || httpResponse.statusCode() == 201) {
            System.out.println("Документ успешно создан!");
        } else {
            System.out.println("Ошибка! Документ не создан!");
        }
    }

    public final static class UtilParseJson {

        public static Document JsonStringToDocument(final String requestBody) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                return objectMapper.readValue(requestBody, Document.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String DocumentToJsonString(final Document document) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                return objectMapper.writeValueAsString(document);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private final static class Document {
        ParticipantInn description;
        String doc_id;
        String doc_status;
        String doc_type;
        String importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        String production_date;
        String production_type;
        List<Product> products = new ArrayList();
        String reg_date;
        String reg_number;

        public ParticipantInn getDescription() {
            return description;
        }

        public void setDescription(ParticipantInn description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public String getImportRequest() {
            return importRequest;
        }

        public void setImportRequest(String importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }
    }

    private final static class ParticipantInn {
        String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    private final static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;

        public String getCertificate_document() {
            return certificate_document;
        }

        public void setCertificate_document(String certificate_document) {
            this.certificate_document = certificate_document;
        }

        public String getCertificate_document_date() {
            return certificate_document_date;
        }

        public void setCertificate_document_date(String certificate_document_date) {
            this.certificate_document_date = certificate_document_date;
        }

        public String getCertificate_document_number() {
            return certificate_document_number;
        }

        public void setCertificate_document_number(String certificate_document_number) {
            this.certificate_document_number = certificate_document_number;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getTnved_code() {
            return tnved_code;
        }

        public void setTnved_code(String tnved_code) {
            this.tnved_code = tnved_code;
        }

        public String getUit_code() {
            return uit_code;
        }

        public void setUit_code(String uit_code) {
            this.uit_code = uit_code;
        }

        public String getUitu_code() {
            return uitu_code;
        }

        public void setUitu_code(String uitu_code) {
            this.uitu_code = uitu_code;
        }

        private String uit_code;
        private String uitu_code;
    }


    public static void main(String[] args) {
        String signature = "some signature";
        String requestBody = "{\"description\":\n" +
                "{ \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\",\n" +
                "\"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true,\n" +
                "\"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\":\n" +
                "\"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\",\n" +
                "\"products\": [ { \"certificate_document\": \"string\",\n" +
                "\"certificate_document_date\": \"2020-01-23\",\n" +
                "\"certificate_document_number\": \"string\", \"owner_inn\": \"string\",\n" +
                "\"producer_inn\": \"string\", \"production_date\": \"2020-01-23\",\n" +
                "\"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ],\n" +
                "\"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
        //Сделаем ограниечение на 3 вызова в секнуду
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 3);
        //Создаем Document из строки для createDocument
        Document document = UtilParseJson.JsonStringToDocument(requestBody);
        //Нагружаем наш метод 50 потоками и смотрим результат в консоли
        for (int i = 0; i < 50; i++) {
            new Thread(() -> {
                crptApi.createDocument(document, signature);
            }).start();
        }
    }
}