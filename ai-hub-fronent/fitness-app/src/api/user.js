import request from "./request";

export const register = (data) => request.post("/ai-hub/user/register", data);

export const login = (data) => request.post("/ai-hub/user/login", data);

export const logout = () => request.post("/ai-hub/user/logout");

export const getMe = () => request.get("/ai-hub/user/me");
