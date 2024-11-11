package edu.nnu.ali.demo.controller;
import java.net.URLDecoder;
import java.util.Arrays;
import java.lang.System;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import edu.nnu.ali.demo.po.JsonData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.audio.ttsv2.enrollment.Voice;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentService;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
@RestController
public class DashScopeController {
    @GetMapping("dashchat")
    public JsonData chat(String key){
        JsonData data=null;
        try {
            Generation gen = new Generation();
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("You are a helpful assistant.").build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(key)
                    .build();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .model("qwen-max")
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
            data=JsonData.ok(gen.call(param));
        }
        catch (Exception ex){
            data=JsonData.error(300,ex.getMessage());
        }
        return data;
    }
    @GetMapping("dashimage")
    public JsonData imageDash(String key ,String url){
        JsonData data=null;
        try{
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("image", URLDecoder.decode(url)), Collections.singletonMap("text", URLDecoder.decode(key)))).build();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .model("qwen-vl-max")
                    .message(userMessage)
                    .build();
            MultiModalConversationResult result = conv.call(param);
            data=JsonData.ok(result);}
        catch (Exception ex){
            data=JsonData.error(300,ex.getMessage());
        }
        return data;
    }
    @GetMapping("dashaudio")
    public JsonData dashAudio(String key ,String url){
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String prefix = "will";
        String targetModel = "cosyvoice-clone-v1";
        JsonData data=null;
        try{
// 复刻声音
            VoiceEnrollmentService service = new VoiceEnrollmentService(apiKey);
            Voice myVoice = service.createVoice(targetModel, prefix, URLDecoder.decode(url));
            System.out.println("your voice id is " + myVoice.getVoiceId());
// 使用复刻的声音来合成文本为语音
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model(targetModel)
                    .voice(myVoice.getVoiceId())
                    .build();
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
            ByteBuffer audio = synthesizer.call(URLDecoder.decode(key));
// 保存合成的语音到文件
            File parent=new File("src/main/resources/static/audio");
            if(! parent.exists()){
                parent.mkdirs();
            }
            String fileName= UUID.randomUUID()+".mp3";
            File file = new File(parent,fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(audio.array());
            } catch (Exception e) {
                throw new RuntimeException(e);}
            data=JsonData.ok(fileName);
        }
        catch (Exception ex){
            data=JsonData.error(300,ex.getMessage());
        }
        return data;
    }
}