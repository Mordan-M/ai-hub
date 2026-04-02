import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getPlan, regeneratePlan } from "../api/fitness";
import Header from "../components/Header";

const DAY_NAMES = ["", "周一", "周二", "周三", "周四", "周五", "周六", "周日"];

const statusStyle = {
  generating: { color: "#b45309", background: "#fffbeb", border: "1.5px solid #fde68a" },
  done:       { color: "#166534", background: "#f0fdf4", border: "1.5px solid #bbf7d0" },
  failed:     { color: "#991b1b", background: "#fef2f2", border: "1.5px solid #fecaca" },
};
const statusLabel = { generating: "生成中", done: "已完成", failed: "生成失败" };

export default function PlanDetail() {
  const { planId } = useParams();
  const navigate = useNavigate();
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [regenerating, setRegenerating] = useState(false);
  const [activeDay, setActiveDay] = useState(null);
  const pollRef = useRef(null);

  const fetchPlan = async (id) => {
    try {
      const res = await getPlan(id);
      const data = res.data;
      setPlan(data);
      setLoading(false);
      if (data.status === "generating") {
        pollRef.current = setTimeout(() => fetchPlan(id), 3000);
      } else {
        const first = data.days?.find((d) => !d.isRestDay);
        if (first) setActiveDay(first.dayOfWeek);
      }
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlan(planId);
    return () => clearTimeout(pollRef.current);
  }, [planId]);

  const handleRegenerate = async () => {
    setRegenerating(true);
    try {
      const res = await regeneratePlan(planId);
      navigate(`/plan/${res.data}`);
    } catch (err) {
      setError(err.message);
      setRegenerating(false);
    }
  };

  if (loading) return (
    <div style={{ minHeight: "100vh", background: "var(--bg)" }}>
      <Header />
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", height: "60vh", gap: "16px" }}>
        <Spinner />
        <div style={{ color: "var(--text-muted)", fontSize: "14px" }}>加载计划...</div>
      </div>
    </div>
  );

  if (error) return (
    <div style={{ minHeight: "100vh", background: "var(--bg)" }}>
      <Header />
      <div style={{ maxWidth: "560px", margin: "80px auto", padding: "0 24px", textAlign: "center" }}>
        <div style={{ fontSize: "36px", marginBottom: "14px" }}>⚠️</div>
        <div style={{ color: "var(--danger)", fontSize: "15px", marginBottom: "24px" }}>{error}</div>
        <button onClick={() => navigate("/")} style={{ background: "var(--accent)", color: "#fff", border: "none", borderRadius: "10px", padding: "11px 24px", fontSize: "14px", fontWeight: 600 }}>
          返回首页
        </button>
      </div>
    </div>
  );

  const currentDay = plan?.days?.find((d) => d.dayOfWeek === activeDay);

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)" }}>
      <Header />
      <div style={{ maxWidth: "860px", margin: "0 auto", padding: "40px 24px" }}>

        {/* Header */}
        <div className="fade-up" style={{ marginBottom: "28px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "16px", flexWrap: "wrap" }}>
            <div>
              <div style={{ fontSize: "12px", color: "var(--text-muted)", fontWeight: 600, letterSpacing: "0.5px", marginBottom: "6px", textTransform: "uppercase" }}>
                {plan.weekStartDate} 起
              </div>
              <h1 style={{ fontSize: "26px", fontWeight: 700, color: "var(--text)", lineHeight: 1.2 }}>
                {plan.title || "训练计划"}
              </h1>
            </div>
            <div style={{ display: "flex", gap: "10px", alignItems: "center", flexShrink: 0 }}>
              <span style={{ fontSize: "12px", fontWeight: 600, borderRadius: "20px", padding: "4px 12px", ...statusStyle[plan.status] }}>
                {statusLabel[plan.status]}
              </span>
              <button onClick={handleRegenerate} disabled={regenerating || plan.status === "generating"} style={{
                background: "var(--surface)", border: "1.5px solid var(--border)",
                color: "var(--text-muted)", borderRadius: "8px", padding: "7px 14px",
                fontSize: "13px", fontWeight: 500,
                opacity: (regenerating || plan.status === "generating") ? 0.5 : 1,
                cursor: (regenerating || plan.status === "generating") ? "default" : "pointer",
              }}>
                {regenerating ? "生成中..." : "重新生成"}
              </button>
            </div>
          </div>

          {plan.summary && (
            <div style={{ marginTop: "14px", background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "12px", padding: "16px 18px", fontSize: "14px", color: "var(--text-muted)", lineHeight: 1.8 }}>
              {plan.summary}
            </div>
          )}
        </div>

        {/* Generating */}
        {plan.status === "generating" && (
          <div className="fade-up fade-up-delay-1" style={{ background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "16px", padding: "64px 32px", textAlign: "center" }}>
            <Spinner size={40} />
            <div style={{ marginTop: "20px", fontSize: "16px", fontWeight: 700, color: "var(--text)" }}>AI 正在制定你的专属计划</div>
            <div style={{ color: "var(--text-muted)", fontSize: "14px", marginTop: "8px" }}>通常需要 10–30 秒，请稍候</div>
            <div style={{ display: "flex", justifyContent: "center", gap: "6px", marginTop: "20px" }}>
              {[0, 1, 2].map((i) => (
                <div key={i} style={{ width: "7px", height: "7px", borderRadius: "50%", background: "var(--accent)", animation: `pulse-dot 1.2s ease-in-out ${i * 0.2}s infinite` }} />
              ))}
            </div>
          </div>
        )}

        {/* Failed */}
        {plan.status === "failed" && (
          <div style={{ background: "var(--danger-bg)", border: "1.5px solid var(--danger-border)", borderRadius: "16px", padding: "40px", textAlign: "center" }}>
            <div style={{ fontSize: "32px", marginBottom: "12px" }}>😞</div>
            <div style={{ color: "var(--danger)", fontSize: "15px", fontWeight: 600, marginBottom: "20px" }}>计划生成失败，请重新尝试</div>
            <button onClick={handleRegenerate} disabled={regenerating} style={{ background: "var(--accent)", color: "#fff", border: "none", borderRadius: "10px", padding: "11px 28px", fontSize: "14px", fontWeight: 600 }}>
              重新生成
            </button>
          </div>
        )}

        {/* Done */}
        {plan.status === "done" && plan.days && (
          <>
            {/* Day tabs */}
            <div className="fade-up fade-up-delay-1" style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: "8px", marginBottom: "20px" }}>
              {plan.days.map((day) => (
                <button key={day.dayOfWeek} onClick={() => !day.isRestDay && setActiveDay(day.dayOfWeek)} style={{
                  background: activeDay === day.dayOfWeek ? "var(--accent)" : day.isRestDay ? "var(--surface2)" : "var(--surface)",
                  border: `1.5px solid ${activeDay === day.dayOfWeek ? "var(--accent)" : day.isRestDay ? "var(--border)" : "var(--border)"}`,
                  borderRadius: "10px", padding: "11px 6px", cursor: day.isRestDay ? "default" : "pointer",
                  transition: "all 0.15s", color: activeDay === day.dayOfWeek ? "#fff" : "var(--text)",
                }}>
                  <div style={{ fontSize: "11px", fontWeight: 600, marginBottom: "5px", color: activeDay === day.dayOfWeek ? "rgba(255,255,255,0.8)" : "var(--text-muted)" }}>
                    {DAY_NAMES[day.dayOfWeek]}
                  </div>
                  {day.isRestDay ? (
                    <div style={{ fontSize: "10px", color: "var(--text-light)" }}>休息</div>
                  ) : (
                    <div style={{ fontSize: "10px", fontWeight: 600, lineHeight: 1.4, color: activeDay === day.dayOfWeek ? "#fff" : "var(--accent)" }}>
                      {day.focusMuscleGroup}
                    </div>
                  )}
                </button>
              ))}
            </div>

            {/* Day detail */}
            {currentDay && (
              <div className="fade-up fade-up-delay-2">
                {(currentDay.warmUpNotes || currentDay.coolDownNotes) && (
                  <div style={{ background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "12px", padding: "18px 20px", marginBottom: "14px", display: "flex", gap: "24px", flexWrap: "wrap" }}>
                    {currentDay.warmUpNotes && (
                      <div style={{ flex: 1, minWidth: "180px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--accent)", letterSpacing: "0.5px", textTransform: "uppercase", marginBottom: "6px" }}>热身</div>
                        <div style={{ fontSize: "13px", color: "var(--text-muted)", lineHeight: 1.7 }}>{currentDay.warmUpNotes}</div>
                      </div>
                    )}
                    {currentDay.coolDownNotes && (
                      <div style={{ flex: 1, minWidth: "180px" }}>
                        <div style={{ fontSize: "11px", fontWeight: 600, color: "var(--accent)", letterSpacing: "0.5px", textTransform: "uppercase", marginBottom: "6px" }}>拉伸放松</div>
                        <div style={{ fontSize: "13px", color: "var(--text-muted)", lineHeight: 1.7 }}>{currentDay.coolDownNotes}</div>
                      </div>
                    )}
                  </div>
                )}

                <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                  {currentDay.exercises?.map((ex, idx) => (
                    <div key={idx} style={{
                      background: "var(--surface)", border: "1.5px solid var(--border)",
                      borderRadius: "14px", padding: "18px 20px",
                      animation: `fadeUp 0.3s ease ${idx * 0.05}s both`,
                    }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "16px", flexWrap: "wrap" }}>
                        <div style={{ flex: 1 }}>
                          <div style={{ display: "flex", alignItems: "baseline", gap: "10px", flexWrap: "wrap", marginBottom: "6px" }}>
                            <span style={{ fontWeight: 700, fontSize: "15px", color: "var(--text)" }}>{ex.nameZh}</span>
                            <span style={{ fontSize: "12px", color: "var(--text-light)", fontStyle: "italic" }}>{ex.nameEn}</span>
                          </div>
                          {ex.coachNotes && (
                            <div style={{ fontSize: "13px", color: "var(--text-muted)", lineHeight: 1.7, borderLeft: "3px solid var(--accent-mid)", paddingLeft: "10px" }}>
                              {ex.coachNotes}
                            </div>
                          )}
                        </div>
                        <div style={{ display: "flex", gap: "8px", flexShrink: 0 }}>
                          <Stat label="组数" value={`${ex.sets}`} unit="组" />
                          <Stat label="次数" value={ex.reps} />
                          <Stat label="休息" value={`${ex.restSeconds}s`} />
                        </div>
                      </div>

                      {ex.bilibiliUrl && (
                        <div style={{ marginTop: "12px", paddingTop: "12px", borderTop: "1.5px solid var(--border)" }}>
                          <a href={ex.bilibiliUrl} target="_blank" rel="noreferrer" style={{
                            display: "inline-flex", alignItems: "center", gap: "6px",
                            color: "#0070f3", textDecoration: "none", fontSize: "13px", fontWeight: 600,
                          }}>
                            ▶ B站搜索教程
                          </a>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value, unit }) {
  return (
    <div style={{ background: "var(--surface2)", border: "1.5px solid var(--border)", borderRadius: "9px", padding: "8px 12px", textAlign: "center", minWidth: "52px" }}>
      <div style={{ fontSize: "11px", color: "var(--text-muted)", fontWeight: 500, marginBottom: "3px" }}>{label}</div>
      <div style={{ fontSize: "14px", fontWeight: 700, color: "var(--accent)" }}>{value}{unit}</div>
    </div>
  );
}

function Spinner({ size = 30 }) {
  return (
    <div style={{
      width: size, height: size,
      border: "2.5px solid var(--border)",
      borderTop: "2.5px solid var(--accent)",
      borderRadius: "50%",
      animation: "spin 0.8s linear infinite",
      margin: "0 auto",
    }} />
  );
}
