import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.vision.v1.*;

import java.time.LocalDateTime;
import java.util.*;

public class ImageProcessor {

    private final Firestore db;
    private final Translate translate;

    public ImageProcessor() {
        this.db = FirestoreOptions.newBuilder().setDatabaseId("cn-t3g12-labels").build().getService();
        this.translate = TranslateOptions.getDefaultInstance().getService();
    }

    public void processImage(ProcessingRequest request) throws Exception {
        try {
            System.out.println("A processar imagem:");
            System.out.println("Request ID: " + request.requestId);
            System.out.println("Bucket: " + request.bucketName);
            System.out.println("Blob: " + request.blobName);

            updateStatus(request.requestId, "PROCESSING", "");

            List<Map<String, Object>> labels = detectAndTranslateLabels(
                    request.bucketName,
                    request.blobName
            );

            List<String> labelsEn = new ArrayList<>();
            List<String> labelsPt = new ArrayList<>();

            for (Map<String, Object> label : labels) {
                labelsEn.add((String) label.get("labelEn"));
                labelsPt.add((String) label.get("labelPt"));
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "DONE");
            updates.put("processedAt", LocalDateTime.now().toString());
            updates.put("labels", labels);
            updates.put("labelsEn", labelsEn);
            updates.put("labelsPt", labelsPt);
            updates.put("errorMessage", "");

            db.collection("requests")
                    .document(request.requestId)
                    .update(updates)
                    .get();

            System.out.println("Resultado guardado no Firestore.");

        } catch (Exception e) {
            updateStatus(request.requestId, "ERROR", e.getMessage());
            throw e;
        }
    }

    private List<Map<String, Object>> detectAndTranslateLabels(
            String bucketName,
            String blobName
    ) throws Exception {

        String gcsUri = "gs://" + bucketName + "/" + blobName;

        ImageSource imageSource = ImageSource.newBuilder()
                .setGcsImageUri(gcsUri)
                .build();

        Image image = Image.newBuilder()
                .setSource(imageSource)
                .build();

        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.LABEL_DETECTION)
                .setMaxResults(10)
                .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();

        List<Map<String, Object>> resultLabels = new ArrayList<>();

        try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response =
                    vision.batchAnnotateImages(List.of(request));

            AnnotateImageResponse imageResponse = response.getResponses(0);

            if (imageResponse.hasError()) {
                throw new RuntimeException(
                        "Erro Vision API: " + imageResponse.getError().getMessage()
                );
            }

            for (EntityAnnotation annotation : imageResponse.getLabelAnnotationsList()) {
                String labelEn = annotation.getDescription();
                float confidence = annotation.getScore();

                Translation translation = translate.translate(
                        labelEn,
                        Translate.TranslateOption.sourceLanguage("en"),
                        Translate.TranslateOption.targetLanguage("pt")
                );

                String labelPt = translation.getTranslatedText();

                Map<String, Object> labelMap = new HashMap<>();
                labelMap.put("labelEn", labelEn);
                labelMap.put("labelPt", labelPt);
                labelMap.put("confidence", confidence);

                resultLabels.add(labelMap);

                System.out.println(labelEn + " -> " + labelPt + " | " + confidence);
            }
        }

        return resultLabels;
    }

    private void updateStatus(
            String requestId,
            String status,
            String errorMessage
    ) throws Exception {

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("errorMessage", errorMessage);

        db.collection("requests")
                .document(requestId)
                .update(updates)
                .get();
    }
}