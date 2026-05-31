package com.madan.M360_Task_1.ai;

import java.util.List;
import java.util.Map;

public class AgUiParameters {
    private String message;
    private String chatId;
    private List<FrontendToolDefinition> frontendTools;
    private UiState uiState;

    public UiState getUiState() {
        return uiState;
    }

    public void setUiState(UiState uiState) {
        this.uiState = uiState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<FrontendToolDefinition> getFrontendTools() {
        return frontendTools;
    }

    public void setFrontendTools(List<FrontendToolDefinition> frontendTools) {
        this.frontendTools = frontendTools;
    }

    public static class FrontendToolDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    public static class UiState {
        private String currentPage;  // e.g. "/chat", "/users", "/approvals"
        private String theme;        // "dark" or "light"
        private String userRole;     // "ADMIN" or "USER"
        private String username;     // logged in username

        public String getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(String currentPage) {
            this.currentPage = currentPage;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }

        public String getUserRole() {
            return userRole;
        }

        public void setUserRole(String userRole) {
            this.userRole = userRole;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
