import { Navigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

export const PrivateRoute = ({ children }) => {
  const token = useAuthStore((s) => s.token) || localStorage.getItem("token");
  return token ? children : <Navigate to="/login" replace />;
};

export const GuestRoute = ({ children }) => {
  const token = useAuthStore((s) => s.token) || localStorage.getItem("token");
  return !token ? children : <Navigate to="/" replace />;
};
