package edu.nnu.ali.demo.controller;
import com.alibaba.cloud.ai.tongyi.audio.speech.TongYiAudioSpeechModel;
import com.alibaba.cloud.ai.tongyi.audio.speech.TongYiAudioSpeechOptions;
import com.alibaba.cloud.ai.tongyi.audio.speech.api.SpeechPrompt;
import com.alibaba.cloud.ai.tongyi.audio.speech.api.SpeechResponse;
import com.alibaba.cloud.ai.tongyi.audio.transcription.TongYiAudioTranscriptionModel;
import com.alibaba.cloud.ai.tongyi.audio.transcription.TongYiAudioTranscriptionOptions;
import com.alibaba.cloud.ai.tongyi.audio.transcription.api.AudioTranscriptionPrompt;
import com.alibaba.cloud.ai.tongyi.audio.transcription.api.AudioTranscriptionResponse;
import com.alibaba.cloud.ai.tongyi.chat.TongYiChatModel;
import com.alibaba.cloud.ai.tongyi.image.TongYiImagesModel;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.nnu.ali.demo.po.JsonData;
import jakarta.annotation.Resource;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.UUID;
import java.util.stream.Stream;

@RestController
public class TongyiController {

    @Resource
    private TongYiChatModel chatModel;
    @Autowired
    private Transcription transcription;


    @GetMapping("chat")
    public JsonData chat(String key){
        JsonData data=null;
        try {
            String s= chatModel.call(key);
            data=JsonData.ok(s);
        }
        catch (Exception ex){
            data=JsonData.error(200,ex.getMessage());
        }
        return data;
    }
    @Resource
    private TongYiImagesModel imagesModel;
    @GetMapping("image")
    public JsonData image(String key){
        JsonData data=null;
        try {
            ImageResponse call = imagesModel.call(new ImagePrompt(key, ImageOptionsBuilder.builder()
                    .withModel("wanx-v1")
                    .withN(1)
                    .withHeight(512)
                    .withWidth(512)
                    .build()));

           data=JsonData.ok(call.getResult().getOutput());


        }
        catch (Exception ex) {
            ex.printStackTrace();
            data=JsonData.error(300,ex.getMessage());
        }
    return  data;
    }
    @Resource
    private TongYiAudioSpeechModel speechModel;
    @GetMapping("speech")
    public JsonData speech(String key){
        JsonData data=null;
        try{
            FileOutputStream outputStream= null;
           File parent =new File("src/main/resources/static/audio");
           if(!parent.exists()){
               parent.mkdirs();
           }
            String fileName = UUID.randomUUID().toString()+".mp3";
           SpeechResponse call= speechModel.call(new SpeechPrompt(key, TongYiAudioSpeechOptions.builder()
                    .withModel("cosyvoice-v1")
                    .withFormat(SpeechSynthesisAudioFormat.MP3)
                    .build()));
           byte[] array =call.getResult().getOutput().array();
           outputStream = new FileOutputStream(new File(parent,fileName));
           outputStream.write(array);
           data = JsonData.ok(fileName);


        }
        catch (Exception ex){
            data=JsonData.error(300,ex.getMessage());
        }
        return data;
    }
    @Resource
    private TongYiAudioTranscriptionModel transcriptionModel;
    @GetMapping("trans")
    public JsonData trans(String key)
    {
        JsonData data=null;
        try{
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            UrlResource resource = new UrlResource(decodedKey);
            AudioTranscriptionResponse call = transcriptionModel.call(
                    new AudioTranscriptionPrompt(resource));
            URL url=new URL(call.getResult().getOutput());
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            StringBuilder sb = new StringBuilder();
            BufferedInputStream in = new BufferedInputStream(
                    connection.getInputStream());
            byte[] buffer = new byte[1024];
            int count;
            while ((count= in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, count));
            }
            JsonObject parse = JsonUtils.parse(sb.toString());
            JsonArray transcripts = parse.getAsJsonArray("transcripts");
            JsonElement jsonElement = transcripts.get(0);
            JsonElement text = jsonElement.getAsJsonObject().get("text");
            data=JsonData.ok(text.toString());
        }
        catch (Exception ex){
            ex.printStackTrace();
            data=JsonData.error(300,ex.getMessage());
        }
        return data;
    }
}
