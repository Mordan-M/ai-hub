import request from "./request";

export const register = (data) => request.post("/api/v1/user/register", data);

export const login = (data) => request.post("/api/v1/user/login", data);

export const logout = () => request.post("/api/v1/user/logout");

export const getMe = () => request.get("/api/v1/user/me");
