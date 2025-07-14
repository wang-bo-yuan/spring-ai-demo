package com.example.demo;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;


import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/helloworld")
public class HelloWorldController {
    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    private static final String DEFAULT_SUMMARY_PROMPT = "请把{document}内容智能的抽取成若干模块，是你输出要带着这些原文的Html标签，要求抽取内容必须是给你的原文，抽取模块的数量不做限制，不能强行抽取！";
    //private final ChatClient dashScopeChatClient;


    @Autowired
    @Qualifier("ollamaChatModel")
    private ChatModel ollama;

    private ChatClient ollamaChat;

    private ChatClient aliChat;
    //private ChatClient ollamaChatClient = ChatClient.create(ollama);


    @Value("classpath:/text-summarize-cn.st")
    private Resource summaridashScopeChatClientzeTemplate;
/*
    public HelloWorldController(Chat chatClientBuilder) {
        this.chatModel = chatClientBuilder
                .defaultSystem(DEFAULT_PROMPT)
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OllamaOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }*/

    public HelloWorldController(@Autowired @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
                                @Autowired @Qualifier("dashscopeChatModel")ChatModel dashScopeChatModel){
        this.ollamaChat = ChatClient.builder(dashScopeChatModel).defaultSystem(DEFAULT_PROMPT)
                // 实现 Logger 的 Advisor
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        OllamaOptions.builder()
                                .topP(0.7)
                                .build()
                )
                .build();
    }




    /**
     * ChatClient 简单调用
     */
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？")String query) {

        return ollamaChat.prompt(query).call().content();
    }

    @PostMapping(path = "/summarize", produces = "text/plain")
    public String summarize(@RequestParam("file") MultipartFile file) throws IOException, TikaException, SAXException {
        //OfficeParser officeParser = new OfficeParser();
        AutoDetectParser parser = new AutoDetectParser();
        ToXMLContentHandler bodyContentHandler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        parser.parse(file.getInputStream(),bodyContentHandler,metadata,parseContext);
        System.out.println(bodyContentHandler);
        List<Document> documents = new TikaDocumentReader(file.getResource()).get();
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        //TextSplitter textSplitter = new TokenTextSplitter();
        //textSplitter.
        String documentText = documents.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));
        //System.out.println("Document text: "+ documentText);

        return ollamaChat.prompt()
                .user(systemSpec ->
                        systemSpec.text(DEFAULT_SUMMARY_PROMPT).param("document", bodyContentHandler))
                .system(systemSpec ->
                        systemSpec.text("你是一个智能的文档处理助手"))
                .call()
                .content();
    }

}
