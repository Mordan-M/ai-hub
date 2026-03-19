import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { PrivateRoute, GuestRoute } from "./router/guards";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Home from "./pages/Home";
import NewPlan from "./pages/NewPlan";
import PlanDetail from "./pages/PlanDetail";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />
        <Route path="/" element={<PrivateRoute><Home /></PrivateRoute>} />
        <Route path="/plan/new" element={<PrivateRoute><NewPlan /></PrivateRoute>} />
        <Route path="/plan/:planId" element={<PrivateRoute><PlanDetail /></PrivateRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
