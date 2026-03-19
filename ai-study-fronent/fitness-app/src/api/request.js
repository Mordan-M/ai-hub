import axios from "axios";

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const SUCCESS_CODES = new Set([0]);

request.interceptors.response.use(
  (res) => {
    const data = res.data;
    if (!SUCCESS_CODES.has(data.code)) {
      return Promise.reject(new Error(data.message || "请求失败"));
    }
    return data;
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(new Error(err.response?.data?.message || err.message || "网络错误"));
  }
);

export default request;
