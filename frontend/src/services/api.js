import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("@PortalPartners:token");
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem("@PortalPartners:token");
      localStorage.removeItem("@PortalPartners:user");
    }
    return Promise.reject(error);
  }
);

export default api;