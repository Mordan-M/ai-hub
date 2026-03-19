import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register as registerApi } from "../api/user";

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", password: "", nickname: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await registerApi(form);
      navigate("/login");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)", display: "flex", alignItems: "center", justifyContent: "center", padding: "24px" }}>
      <div style={{ width: "100%", maxWidth: "420px" }}>

        <div className="fade-up" style={{ marginBottom: "36px" }}>
          <div style={{ fontFamily: "'Lora'", fontSize: "28px", fontWeight: 600, color: "var(--accent)", marginBottom: "12px" }}>FitAI</div>
          <h1 style={{ fontSize: "22px", fontWeight: 700, color: "var(--text)", marginBottom: "6px" }}>创建账户</h1>
          <p style={{ color: "var(--text-muted)", fontSize: "14px" }}>几步设置，开启你的 AI 训练之旅</p>
        </div>

        <div className="fade-up fade-up-delay-1" style={{ background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "16px", padding: "32px" }}>
          {error && (
            <div style={{ background: "var(--danger-bg)", border: "1.5px solid var(--danger-border)", color: "var(--danger)", borderRadius: "10px", padding: "11px 14px", fontSize: "13px", marginBottom: "20px" }}>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            <div>
              <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--text)", display: "block", marginBottom: "7px" }}>
                用户名 <span style={{ color: "var(--accent)" }}>*</span>
              </label>
              <input type="text" placeholder="3-20 个字符" value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })} minLength={3} maxLength={20} required />
            </div>
            <div>
              <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--text)", display: "block", marginBottom: "7px" }}>
                密码 <span style={{ color: "var(--accent)" }}>*</span>
              </label>
              <input type="password" placeholder="6-30 个字符" value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })} minLength={6} maxLength={30} required />
            </div>
            <div>
              <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--text)", display: "block", marginBottom: "7px" }}>
                昵称 <span style={{ color: "var(--text-light)", fontWeight: 400 }}>（可选）</span>
              </label>
              <input type="text" placeholder="你的显示名称" value={form.nickname}
                onChange={(e) => setForm({ ...form, nickname: e.target.value })} />
            </div>
            <button type="submit" disabled={loading} style={{
              marginTop: "6px", background: loading ? "var(--border-strong)" : "var(--accent)",
              color: "#fff", border: "none", borderRadius: "10px", padding: "13px",
              fontSize: "14px", fontWeight: 600, opacity: loading ? 0.75 : 1,
            }}>
              {loading ? "注册中..." : "创建账户"}
            </button>
          </form>

          <div style={{ marginTop: "24px", textAlign: "center", fontSize: "14px", color: "var(--text-muted)" }}>
            已有账户？{" "}
            <Link to="/login" style={{ color: "var(--accent)", textDecoration: "none", fontWeight: 600 }}>立即登录</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
