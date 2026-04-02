import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getLatestPlan } from "../api/fitness";
import { useAuthStore } from "../store/authStore";
import Header from "../components/Header";

const DAY_NAMES = ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"];
const statusLabel = { generating: "生成中", done: "已完成", failed: "生成失败" };
const statusStyle = {
  generating: { color: "#b45309", background: "#fffbeb", border: "1.5px solid #fde68a" },
  done:       { color: "#166534", background: "#f0fdf4", border: "1.5px solid #bbf7d0" },
  failed:     { color: "#991b1b", background: "#fef2f2", border: "1.5px solid #fecaca" },
};

export default function Home() {
  const user = useAuthStore((s) => s.user);
  const navigate = useNavigate();
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getLatestPlan()
      .then((res) => setPlan(res.data))
      .catch(() => setPlan(null))
      .finally(() => setLoading(false));
  }, []);

  const hour = new Date().getHours();
  const greeting = hour < 12 ? "早上好" : hour < 18 ? "下午好" : "晚上好";

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)" }}>
      <Header />
      <div style={{ maxWidth: "820px", margin: "0 auto", padding: "44px 24px" }}>

        {/* Welcome */}
        <div className="fade-up" style={{ marginBottom: "36px" }}>
          <h1 style={{ fontSize: "28px", fontWeight: 700, color: "var(--text)", marginBottom: "6px" }}>
            {greeting}，{user?.nickname || user?.username} 👋
          </h1>
          <p style={{ color: "var(--text-muted)", fontSize: "15px" }}>
            准备好今天的训练了吗？
          </p>
        </div>

        {/* Quick actions */}
        <div className="fade-up fade-up-delay-1" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px", marginBottom: "36px" }}>
          <button onClick={() => navigate("/plan/new")} style={{
            background: "var(--accent)", border: "none", color: "#fff",
            borderRadius: "14px", padding: "24px 22px", textAlign: "left", cursor: "pointer",
          }}>
            <div style={{ fontSize: "24px", marginBottom: "10px" }}>✦</div>
            <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "4px" }}>生成新计划</div>
            <div style={{ fontSize: "13px", opacity: 0.82 }}>填写偏好，AI 定制一周方案</div>
          </button>

          <button onClick={() => plan && navigate(`/plan/${plan.planId}`)} style={{
            background: "var(--surface)", border: "1.5px solid var(--border)", color: "var(--text)",
            borderRadius: "14px", padding: "24px 22px", textAlign: "left",
            cursor: plan ? "pointer" : "default", opacity: plan ? 1 : 0.55,
          }}>
            <div style={{ fontSize: "24px", marginBottom: "10px" }}>📋</div>
            <div style={{ fontSize: "16px", fontWeight: 700, marginBottom: "4px" }}>最新计划</div>
            <div style={{ fontSize: "13px", color: "var(--text-muted)" }}>{plan ? plan.title : "暂无计划"}</div>
          </button>
        </div>

        {/* Plan preview */}
        {loading ? (
          <div className="fade-up fade-up-delay-2" style={{ textAlign: "center", padding: "60px", color: "var(--text-muted)", fontSize: "14px" }}>
            加载中...
          </div>
        ) : plan ? (
          <div className="fade-up fade-up-delay-2">
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <span style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", letterSpacing: "0.5px", textTransform: "uppercase" }}>最近计划</span>
              <span style={{ fontSize: "12px", fontWeight: 600, borderRadius: "20px", padding: "3px 12px", ...statusStyle[plan.status] }}>
                {statusLabel[plan.status]}
              </span>
            </div>

            <div style={{ background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "14px", padding: "22px", marginBottom: "14px" }}>
              <div style={{ fontWeight: 700, fontSize: "16px", marginBottom: "8px", color: "var(--text)" }}>{plan.title}</div>
              <div style={{ color: "var(--text-muted)", fontSize: "13px", lineHeight: 1.75 }}>{plan.summary}</div>
            </div>

            {/* 7-day strip */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "8px" }}>
              {plan.days?.map((day) => (
                <div key={day.dayOfWeek} style={{
                  background: day.isRestDay ? "var(--surface2)" : "var(--surface)",
                  border: `1.5px solid ${day.isRestDay ? "var(--border)" : "var(--accent-mid)"}`,
                  borderRadius: "10px", padding: "10px 6px", textAlign: "center",
                }}>
                  <div style={{ fontSize: "11px", color: "var(--text-muted)", fontWeight: 500, marginBottom: "5px" }}>
                    {DAY_NAMES[day.dayOfWeek]}
                  </div>
                  {day.isRestDay ? (
                    <div style={{ fontSize: "11px", color: "var(--text-light)" }}>休息</div>
                  ) : (
                    <div style={{ fontSize: "10px", color: "var(--accent)", fontWeight: 600, lineHeight: 1.4 }}>
                      {day.focusMuscleGroup}
                    </div>
                  )}
                </div>
              ))}
            </div>

            <div style={{ marginTop: "18px", textAlign: "center" }}>
              <Link to={`/plan/${plan.planId}`} style={{ color: "var(--accent)", textDecoration: "none", fontSize: "14px", fontWeight: 600 }}>
                查看完整计划 →
              </Link>
            </div>
          </div>
        ) : (
          <div className="fade-up fade-up-delay-2" style={{
            background: "var(--surface)", border: "1.5px dashed var(--border-strong)",
            borderRadius: "16px", padding: "52px 32px", textAlign: "center",
          }}>
            <div style={{ fontSize: "36px", marginBottom: "14px" }}>🏋️</div>
            <div style={{ fontSize: "16px", fontWeight: 700, color: "var(--text)", marginBottom: "8px" }}>还没有训练计划</div>
            <div style={{ color: "var(--text-muted)", fontSize: "14px", marginBottom: "24px" }}>
              告诉 AI 你的目标，生成专属一周方案
            </div>
            <button onClick={() => navigate("/plan/new")} style={{
              background: "var(--accent)", color: "#fff", border: "none",
              borderRadius: "10px", padding: "11px 28px", fontSize: "14px", fontWeight: 600,
            }}>
              开始生成
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
