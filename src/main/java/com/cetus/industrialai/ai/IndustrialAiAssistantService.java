package com.cetus.industrialai.ai;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import java.util.List;

//@AiService
public interface IndustrialAiAssistantService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatForReport(String userMessage);

    // 学习报告
    record Report(String name, List<String> suggestionList){}

//    //自定义JSON
//    ResponseFormat responseFormat = ResponseFormat.builder()
//            .type(JSON)
//            .jsonSchema(JsonSchema.builder()
//                    .name("Person")
//                    .rootElement(JsonObjectSchema.builder()
//                            .addStringProperty("name")
//                            .addIntegerProperty("age")
//                            .addNumberProperty("height")
//                            .addBooleanProperty("married")
//                            .required("name", "age", "height", "married")
//                            .build())
//                    .build())
//            .build();
//    ChatRequest chatRequest = ChatRequest.builder()
//            .responseFormat(responseFormat)
//            .messages(userMessage)
//            .build();

    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String userMessage);


}
