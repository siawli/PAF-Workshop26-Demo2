package revision.workshop26.demo264.controller;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.json.Json;
import jakarta.json.JsonObject;

@RestController
public class UploadRestController {

    @Autowired
    private AmazonS3 s3;

    @PostMapping(path="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PostMapping(path="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("image-file") MultipartFile imageFile, 
        @RequestPart String name, @RequestPart String shortNote) {

        String fileName = imageFile.getOriginalFilename();
        String contentType = imageFile.getContentType();
        Long imageSize = imageFile.getSize();

        byte[] buff = new byte[0];

        try {
            buff = imageFile.getBytes();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        String uuid = UUID.randomUUID().toString().substring(0, 8);

        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.addUserMetadata("Filename", fileName);
        metadata.addUserMetadata("Image-Size", imageSize.toString());
        metadata.addUserMetadata("Short-Note", shortNote);
        metadata.addUserMetadata("Poster", name);
        metadata.addUserMetadata("Timestamp", timestamp.toString());

        JsonObject message = null;

        try {
            PutObjectRequest putReq = new PutObjectRequest(
                "paf-siawli",
                "images/demo2/" + uuid, 
                imageFile.getInputStream(), 
                metadata);
            putReq.setCannedAcl(CannedAccessControlList.PublicRead);
            s3.putObject(putReq);
            message = Json.createObjectBuilder().add("uuid", uuid).build();
            return ResponseEntity.ok(message.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.notFound().build();
        }

    }

    @GetMapping("/blob/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        
        GetObjectRequest getReq = new GetObjectRequest("paf-siawli", "images/demo2/" + id);
        
        S3Object obj = null;

        try {
            obj = s3.getObject(getReq);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

        ObjectMetadata metadata = obj.getObjectMetadata();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Original-Name", metadata.getUserMetaDataOf("Filename"));
        headers.set("Create-Time", metadata.getUserMetaDataOf("Timestamp"));
        headers.set("Uploader", metadata.getUserMetaDataOf("Poster"));
        headers.set("Notes", metadata.getUserMetaDataOf("Short-Note"));
        headers.set("Content-Type", metadata.getContentType());

        byte[] buff = new byte[0];

        try {
            buff = IOUtils.toByteArray(obj.getObjectContent());
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().headers(headers).body(buff);
        
    }
    
}
