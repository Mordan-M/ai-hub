import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { logout as logoutApi } from "../api/user";

export default function Header() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try { await logoutApi(); } catch (_) {}
    logout();
    navigate("/login");
  };

  const initial = (user?.nickname || user?.username || "U")[0].toUpperCase();

  return (
    <header style={{
      background: "var(--surface)",
      borderBottom: "1.5px solid var(--border)",
      padding: "0 32px",
      height: "58px",
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      position: "sticky",
      top: 0,
      zIndex: 100,
    }}>
      <Link to="/" style={{ textDecoration: "none", display: "flex", alignItems: "center", gap: "8px" }}>
        <span style={{ fontFamily: "'Lora'", fontSize: "22px", fontWeight: 600, color: "var(--accent)" }}>FitAI</span>
      </Link>

      <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
        <Link to="/plan/new" style={{
          background: "var(--accent)", color: "#fff", textDecoration: "none",
          borderRadius: "8px", padding: "7px 16px", fontSize: "13px", fontWeight: 600,
        }}>
          + 新计划
        </Link>

        <div style={{ display: "flex", alignItems: "center", gap: "8px", padding: "4px 10px", background: "var(--surface2)", borderRadius: "20px", border: "1.5px solid var(--border)" }}>
          <div style={{
            width: "26px", height: "26px", borderRadius: "50%",
            background: "var(--accent-light)", border: "1.5px solid var(--accent-mid)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "12px", fontWeight: 700, color: "var(--accent)",
          }}>
            {initial}
          </div>
          <span style={{ fontSize: "13px", fontWeight: 500, color: "var(--text)" }}>
            {user?.nickname || user?.username}
          </span>
        </div>

        <button onClick={handleLogout} style={{
          background: "transparent",
          border: "1.5px solid var(--border)",
          color: "var(--text-muted)",
          borderRadius: "8px",
          padding: "6px 14px",
          fontSize: "13px",
          fontWeight: 500,
        }}>
          退出
        </button>
      </div>
    </header>
  );
}
