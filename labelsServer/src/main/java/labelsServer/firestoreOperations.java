package labelsServer;

import cn.labels.contract.ImageSummary;
import cn.labels.contract.LabelInfo;
import cn.labels.contract.imageInfoReply;
import cn.labels.contract.namesByDateReply;
import com.google.cloud.firestore.*;

import java.time.LocalDateTime;
import java.util.*;

public class FirestoreOperations {

    private final Firestore db;

    public FirestoreOperations() {
        this.db = FirestoreOptions.getDefaultInstance().getService();
    }

    public void createPendingRequest(
            String requestId,
            String fileName,
            String bucketName,
            String blobName
    ) throws Exception {

        Map<String, Object> doc = new HashMap<>();

        doc.put("requestId", requestId);
        doc.put("fileName", fileName);
        doc.put("bucketName", bucketName);
        doc.put("blobName", blobName);
        doc.put("status", "PENDING");
        doc.put("submittedAt", LocalDateTime.now().toString());
        doc.put("processedAt", "");
        doc.put("errorMessage", "");
        doc.put("labels", new ArrayList<Map<String, Object>>());
        doc.put("labelsEn", new ArrayList<String>());
        doc.put("labelsPt", new ArrayList<String>());

        db.collection("requests")
                .document(requestId)
                .set(doc)
                .get();

        System.out.println("Pedido criado no Firestore com estado PENDING.");
    }

    public imageInfoReply getImageInfo(String requestId) throws Exception {

        DocumentSnapshot doc = db.collection("requests")
                .document(requestId)
                .get()
                .get();

        if (!doc.exists()) {
            return imageInfoReply.newBuilder()
                    .setRequestId(requestId)
                    .setStatus("NOT_FOUND")
                    .setErrorMessage("Pedido não encontrado no Firestore.")
                    .build();
        }

        imageInfoReply.Builder reply = imageInfoReply.newBuilder()
                .setRequestId(getString(doc, "requestId"))
                .setFileName(getString(doc, "fileName"))
                .setBucketName(getString(doc, "bucketName"))
                .setBlobName(getString(doc, "blobName"))
                .setStatus(getString(doc, "status"))
                .setSubmittedAt(getString(doc, "submittedAt"))
                .setProcessedAt(getString(doc, "processedAt"))
                .setErrorMessage(getString(doc, "errorMessage"));

        List<Map<String, Object>> labels =
                (List<Map<String, Object>>) doc.get("labels");

        if (labels != null) {
            for (Map<String, Object> label : labels) {
                LabelInfo labelInfo = LabelInfo.newBuilder()
                        .setLabelEn((String) label.getOrDefault("labelEn", ""))
                        .setLabelPt((String) label.getOrDefault("labelPt", ""))
                        .setConfidence(((Number) label.getOrDefault("confidence", 0.0)).floatValue())
                        .build();

                reply.addLabels(labelInfo);
            }
        }

        return reply.build();
    }

    public namesByDateReply getNamesByDate(
            String label,
            String startDate,
            String endDate
    ) throws Exception {

        CollectionReference requests = db.collection("requests");

        Query query = requests
                .whereGreaterThanOrEqualTo("processedAt", startDate)
                .whereLessThanOrEqualTo("processedAt", endDate)
                .whereArrayContains("labelsEn", label);

        QuerySnapshot snapshot = query.get().get();

        namesByDateReply.Builder reply = namesByDateReply.newBuilder();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            ImageSummary.Builder image = ImageSummary.newBuilder()
                    .setRequestId(getString(doc, "requestId"))
                    .setFileName(getString(doc, "fileName"))
                    .setBucketName(getString(doc, "bucketName"))
                    .setBlobName(getString(doc, "blobName"))
                    .setProcessedAt(getString(doc, "processedAt"));

            List<Map<String, Object>> labels =
                    (List<Map<String, Object>>) doc.get("labels");

            if (labels != null) {
                for (Map<String, Object> labelMap : labels) {
                    LabelInfo labelInfo = LabelInfo.newBuilder()
                            .setLabelEn((String) labelMap.getOrDefault("labelEn", ""))
                            .setLabelPt((String) labelMap.getOrDefault("labelPt", ""))
                            .setConfidence(((Number) labelMap.getOrDefault("confidence", 0.0)).floatValue())
                            .build();

                    image.addLabels(labelInfo);
                }
            }

            reply.addImages(image.build());
        }

        return reply.build();
    }

    private String getString(DocumentSnapshot doc, String fieldName) {
        String value = doc.getString(fieldName);
        return value == null ? "" : value;
    }
}