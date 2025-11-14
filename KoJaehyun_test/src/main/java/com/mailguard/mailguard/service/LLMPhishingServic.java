package com.mailguard.mailguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailguard.mailguard.config.OpenAIConfig;
import com.mailguard.mailguard.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMPhishingService {
    
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void analyzeWithLLM(EmailDto email) {
        try {
            log.info("🤖 LLM 분석 시작: {}", email.getSubject());
            
            String prompt = buildPrompt(email);
            String response = callGPTAPI(prompt);
            parseAndUpdateEmail(email, response);
            
            log.info("✅ LLM 분석 완료: {} → {} ({}점)", 
                    email.getSubject(), email.getRiskLevel(), email.getRiskScore());
            
        } catch (Exception e) {
            log.error("❌ LLM 분석 실패: {}", e.getMessage());
        }
    }
    
    private String buildPrompt(EmailDto email) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 이메일 보안 전문가입니다. ");
        prompt.append("다음 이메일이 피싱/스미싱 메일인지 분석해주세요.\n\n");
        
        // ===== ✨ 추가: 중요한 가이드라인 =====
        prompt.append("**주의사항:**\n");
        prompt.append("- 'noreply' 주소는 정상적인 자동발송 시스템에서도 흔히 사용됩니다.\n");
        prompt.append("- 금융기관의 정상적인 정책 안내와 피싱 메일을 신중하게 구분해야 합니다.\n");
        prompt.append("- 공식 도메인(@naver.com, @naverpay.com, @google.com 등)에서 온 메일은 신뢰도가 높습니다.\n");
        prompt.append("- 고객확인제도나 정책 시행 안내는 정상적인 업무 메일일 가능성이 높습니다.\n");
        prompt.append("- 개인정보 요구가 있어도, 공식 도메인에서 발송되고 공식 사이트로 연결되면 정상일 수 있습니다.\n\n");
        
        prompt.append("**이메일 정보:**\n");
        prompt.append("발신자: ").append(email.getFrom()).append("\n");
        prompt.append("제목: ").append(email.getSubject()).append("\n");
        
        String content = email.getContent();
        if (content != null) {
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            prompt.append("본문: ").append(content).append("\n");
        }
        
        if (email.getExtractedUrls() != null && !email.getExtractedUrls().isEmpty()) {
            prompt.append("포함된 URL: ").append(email.getExtractedUrls()).append("\n");
        }
        
        prompt.append("\n**분석 기준 (우선순위 순):**\n");
        prompt.append("1. 발신자 도메인이 공식 도메인인가? (가장 중요!)\n");
        prompt.append("2. URL이 공식 웹사이트로 연결되는가?\n");
        prompt.append("3. 비정상적인 긴급성이나 협박이 있는가?\n");
        prompt.append("4. 금전적 이득을 약속하는가?\n");
        prompt.append("5. 심각한 맞춤법 오류나 어색한 표현이 있는가?\n");
        prompt.append("6. 의심스러운 첨부파일이 있는가?\n\n");
        
        // ===== ✨ 수정: 더 명확한 판단 기준 =====
        prompt.append("**판단 기준:**\n");
        prompt.append("- SAFE (0-40점): 공식 도메인에서 발송된 정상적인 업무 메일\n");
        prompt.append("- SUSPICIOUS (41-70점): 의심스러운 요소가 있지만 확실하지 않은 경우\n");
        prompt.append("- DANGEROUS (71-100점): 명백한 피싱/사기 징후가 있는 경우\n\n");
        
        prompt.append("**응답 형식 (JSON):**\n");
        prompt.append("{\n");
        prompt.append("  \"isPhishing\": true/false,\n");
        prompt.append("  \"confidence\": 0-100,\n");
        prompt.append("  \"riskLevel\": \"SAFE\"/\"SUSPICIOUS\"/\"DANGEROUS\",\n");
        prompt.append("  \"reasons\": [\"이유1\", \"이유2\"],\n");
        prompt.append("  \"recommendation\": \"사용자 조언\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private String callGPTAPI(String prompt) throws Exception {
        WebClient webClient = WebClient.builder()
                .baseUrl(OpenAIConfig.API_URL)
                .defaultHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAIConfig.getModel());
        requestBody.put("max_tokens", openAIConfig.getMaxTokens());
        requestBody.put("temperature", openAIConfig.getTemperature());
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", List.of(message));
        
        String response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        JsonNode root = objectMapper.readTree(response);
        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
        
        return content;
    }
    
    private void parseAndUpdateEmail(EmailDto email, String gptResponse) {
        try {
            // 마크다운 코드 블록 제거
            String cleanedResponse = gptResponse.trim();
            
            // ```json ... ``` 형식 처리
            if (cleanedResponse.contains("```json")) {
                int startIdx = cleanedResponse.indexOf("```json") + 7;
                int endIdx = cleanedResponse.lastIndexOf("```");
                if (endIdx > startIdx) {
                    cleanedResponse = cleanedResponse.substring(startIdx, endIdx).trim();
                }
            }
            // ``` ... ``` 형식 처리
            else if (cleanedResponse.startsWith("```") && cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3, cleanedResponse.length() - 3).trim();
            }
            
            log.info("정리된 응답: {}", cleanedResponse);
            
            JsonNode analysis = objectMapper.readTree(cleanedResponse);
            
            String llmRiskLevel = analysis.path("riskLevel").asText();
            int llmConfidence = analysis.path("confidence").asInt();
            
            // ===== ✨ 수정: LLM에 더 높은 가중치 (70%) =====
            int ruleScore = email.getRiskScore();
            int finalScore = (int)(ruleScore * 0.3 + llmConfidence * 0.7);
            
            // ===== ✨ 추가: LLM이 SAFE면 점수 제한 =====
            if ("SAFE".equals(llmRiskLevel) && finalScore > 40) {
                finalScore = 40;  // SAFE는 최대 40점
                log.info("LLM이 SAFE로 판단 → 점수 제한: {} → {}", (int)(ruleScore * 0.3 + llmConfidence * 0.7), finalScore);
            }
            
            // ===== ✨ 추가: LLM이 SUSPICIOUS면 중간 범위로 =====
            if ("SUSPICIOUS".equals(llmRiskLevel)) {
                if (finalScore < 41) finalScore = 41;
                if (finalScore > 70) finalScore = 70;
            }
            
            // ===== ✨ 추가: LLM이 DANGEROUS면 최소 71점 =====
            if ("DANGEROUS".equals(llmRiskLevel) && finalScore < 71) {
                finalScore = 71;
            }
            
            email.setRiskLevel(llmRiskLevel);
            email.setRiskScore(finalScore);
            
            JsonNode reasons = analysis.path("reasons");
            if (reasons.isArray()) {
                reasons.forEach(reason -> 
                    email.getDetectedPatterns().add("🤖 LLM: " + reason.asText())
                );
            }
            
            String recommendation = analysis.path("recommendation").asText();
            if (recommendation != null && !recommendation.isEmpty()) {
                email.getDetectedPatterns().add("💡 " + recommendation);
            }
            
        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", e.getMessage());
            log.error("원본 GPT 응답: {}", gptResponse);
        }
    }
}
