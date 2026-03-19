import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { submitPreferences } from "../api/fitness";
import Header from "../components/Header";

const STEPS = ["基本信息", "训练部位", "器材 & 时长", "频率 & 风格", "确认提交"];

const EXPERIENCE_OPTIONS = [
  { value: "beginner", label: "新手", desc: "0–6 个月" },
  { value: "intermediate", label: "进阶", desc: "6 个月–2 年" },
  { value: "advanced", label: "老手", desc: "2 年以上" },
];

const GOAL_OPTIONS = [
  { value: "muscle_gain", label: "增肌", emoji: "💪" },
  { value: "fat_loss", label: "减脂", emoji: "🔥" },
  { value: "body_shaping", label: "塑形", emoji: "✨" },
  { value: "endurance", label: "提升耐力", emoji: "🏃" },
  { value: "general_health", label: "保持健康", emoji: "❤️" },
];

const MUSCLE_OPTIONS = [
  { value: "chest", label: "胸肌" },
  { value: "back", label: "背部" },
  { value: "shoulders", label: "肩膀" },
  { value: "arms", label: "手臂" },
  { value: "core", label: "核心" },
  { value: "legs", label: "腿部" },
  { value: "glutes", label: "臀部" },
  { value: "full_body", label: "全身均衡" },
];

const EQUIPMENT_OPTIONS = [
  { value: "none", label: "无器械", desc: "徒手训练" },
  { value: "dumbbell", label: "哑铃", desc: "家用哑铃" },
  { value: "barbell", label: "杠铃", desc: "杠铃架" },
  { value: "gym_machine", label: "健身房", desc: "全套器械" },
  { value: "home_equipment", label: "家用器材", desc: "综合器械" },
];

const DURATION_OPTIONS = [
  { value: "under_30", label: "30 分钟内" },
  { value: "30_to_60", label: "30–60 分钟" },
  { value: "60_to_90", label: "60–90 分钟" },
  { value: "above_90", label: "90 分钟以上" },
];

const STYLE_OPTIONS = [
  { value: "split", label: "分化训练", desc: "每天专练一个部位" },
  { value: "full_body", label: "全身训练", desc: "每次练全身" },
  { value: "hiit", label: "HIIT", desc: "高强度间歇" },
  { value: "circuit", label: "循环训练", desc: "多动作循环" },
  { value: "strength", label: "力量训练", desc: "重量优先" },
];

const Card = ({ selected, onClick, children }) => (
  <button onClick={onClick} style={{
    background: selected ? "var(--accent-light)" : "var(--surface)",
    border: `1.5px solid ${selected ? "var(--accent)" : "var(--border)"}`,
    borderRadius: "10px", padding: "13px 15px", textAlign: "left",
    cursor: "pointer", color: "var(--text)", transition: "all 0.15s", width: "100%",
  }}>
    {children}
  </button>
);

export default function NewPlan() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    experienceLevel: "", goal: "", focusMuscles: [],
    equipment: "", sessionDuration: "",
    trainingDaysPerWeek: 4, trainingStyle: "", injuryNotes: "",
  });

  const toggleMuscle = (val) => setForm((f) => ({
    ...f,
    focusMuscles: f.focusMuscles.includes(val)
      ? f.focusMuscles.filter((m) => m !== val)
      : [...f.focusMuscles, val],
  }));

  const canNext = () => {
    if (step === 0) return form.experienceLevel && form.goal;
    if (step === 1) return form.focusMuscles.length > 0;
    if (step === 2) return form.equipment && form.sessionDuration;
    if (step === 3) return form.trainingStyle && form.trainingDaysPerWeek;
    return true;
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await submitPreferences(form);
      navigate(`/plan/${res.data}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const labelOf = (opts, val) => opts.find((o) => o.value === val)?.label || "";

  return (
    <div style={{ minHeight: "100vh", background: "var(--bg)" }}>
      <Header />
      <div style={{ maxWidth: "620px", margin: "0 auto", padding: "40px 24px" }}>

        {/* Progress bar */}
        <div style={{ marginBottom: "36px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "10px" }}>
            <span style={{ fontSize: "13px", fontWeight: 600, color: "var(--text)" }}>{STEPS[step]}</span>
            <span style={{ fontSize: "13px", color: "var(--text-muted)" }}>{step + 1} / {STEPS.length}</span>
          </div>
          <div style={{ height: "5px", background: "var(--border)", borderRadius: "99px", overflow: "hidden" }}>
            <div style={{
              height: "100%", borderRadius: "99px", background: "var(--accent)",
              width: `${((step + 1) / STEPS.length) * 100}%`, transition: "width 0.3s ease",
            }} />
          </div>
          <div style={{ display: "flex", gap: "8px", marginTop: "10px" }}>
            {STEPS.map((s, i) => (
              <div key={i} style={{
                flex: 1, fontSize: "11px", textAlign: "center",
                color: i <= step ? "var(--accent)" : "var(--text-light)",
                fontWeight: i === step ? 600 : 400,
              }}>{s}</div>
            ))}
          </div>
        </div>

        {/* Step content */}
        <div style={{ background: "var(--surface)", border: "1.5px solid var(--border)", borderRadius: "16px", padding: "28px", minHeight: "360px" }}>

          {/* Step 0 */}
          {step === 0 && (
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>经验等级</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "10px" }}>
                  {EXPERIENCE_OPTIONS.map((o) => (
                    <Card key={o.value} selected={form.experienceLevel === o.value} onClick={() => setForm({ ...form, experienceLevel: o.value })}>
                      <div style={{ fontWeight: 600, fontSize: "14px", color: form.experienceLevel === o.value ? "var(--accent)" : "var(--text)" }}>{o.label}</div>
                      <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "3px" }}>{o.desc}</div>
                    </Card>
                  ))}
                </div>
              </div>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>训练目标</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  {GOAL_OPTIONS.map((o) => (
                    <Card key={o.value} selected={form.goal === o.value} onClick={() => setForm({ ...form, goal: o.value })}>
                      <span style={{ fontSize: "16px", marginRight: "8px" }}>{o.emoji}</span>
                      <span style={{ fontWeight: 600, fontSize: "14px", color: form.goal === o.value ? "var(--accent)" : "var(--text)" }}>{o.label}</span>
                    </Card>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 1 */}
          {step === 1 && (
            <div>
              <div style={{ fontSize: "13px", color: "var(--text-muted)", marginBottom: "14px" }}>可多选</div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                {MUSCLE_OPTIONS.map((o) => (
                  <Card key={o.value} selected={form.focusMuscles.includes(o.value)} onClick={() => toggleMuscle(o.value)}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <span style={{ fontWeight: 600, fontSize: "14px", color: form.focusMuscles.includes(o.value) ? "var(--accent)" : "var(--text)" }}>{o.label}</span>
                      {form.focusMuscles.includes(o.value) && (
                        <span style={{ width: "18px", height: "18px", borderRadius: "50%", background: "var(--accent)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: "10px", color: "#fff", fontWeight: 700 }}>✓</span>
                      )}
                    </div>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* Step 2 */}
          {step === 2 && (
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>可用器材</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  {EQUIPMENT_OPTIONS.map((o) => (
                    <Card key={o.value} selected={form.equipment === o.value} onClick={() => setForm({ ...form, equipment: o.value })}>
                      <div style={{ fontWeight: 600, fontSize: "14px", color: form.equipment === o.value ? "var(--accent)" : "var(--text)" }}>{o.label}</div>
                      <div style={{ fontSize: "12px", color: "var(--text-muted)", marginTop: "3px" }}>{o.desc}</div>
                    </Card>
                  ))}
                </div>
              </div>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>单次训练时长</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px" }}>
                  {DURATION_OPTIONS.map((o) => (
                    <Card key={o.value} selected={form.sessionDuration === o.value} onClick={() => setForm({ ...form, sessionDuration: o.value })}>
                      <span style={{ fontWeight: 600, fontSize: "14px", color: form.sessionDuration === o.value ? "var(--accent)" : "var(--text)" }}>{o.label}</span>
                    </Card>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 3 */}
          {step === 3 && (
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "14px", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                  每周训练天数 <span style={{ color: "var(--accent)", fontFamily: "monospace" }}>{form.trainingDaysPerWeek} 天</span>
                </div>
                <div style={{ display: "flex", gap: "8px" }}>
                  {[2, 3, 4, 5, 6].map((d) => (
                    <button key={d} onClick={() => setForm({ ...form, trainingDaysPerWeek: d })} style={{
                      flex: 1, padding: "11px 0", borderRadius: "10px",
                      border: `1.5px solid ${form.trainingDaysPerWeek === d ? "var(--accent)" : "var(--border)"}`,
                      background: form.trainingDaysPerWeek === d ? "var(--accent-light)" : "var(--surface)",
                      color: form.trainingDaysPerWeek === d ? "var(--accent)" : "var(--text)",
                      fontWeight: 700, fontSize: "16px", cursor: "pointer",
                    }}>{d}</button>
                  ))}
                </div>
              </div>
              <div>
                <div style={{ fontSize: "13px", fontWeight: 600, color: "var(--text-muted)", marginBottom: "12px", textTransform: "uppercase", letterSpacing: "0.5px" }}>训练风格</div>
                <div style={{ display: "flex", flexDirection: "column", gap: "9px" }}>
                  {STYLE_OPTIONS.map((o) => (
                    <Card key={o.value} selected={form.trainingStyle === o.value} onClick={() => setForm({ ...form, trainingStyle: o.value })}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span style={{ fontWeight: 600, fontSize: "14px", color: form.trainingStyle === o.value ? "var(--accent)" : "var(--text)" }}>{o.label}</span>
                        <span style={{ fontSize: "12px", color: "var(--text-muted)" }}>{o.desc}</span>
                      </div>
                    </Card>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 4 */}
          {step === 4 && (
            <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
              <div style={{ background: "var(--surface2)", borderRadius: "12px", padding: "18px", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px" }}>
                {[
                  ["经验等级", labelOf(EXPERIENCE_OPTIONS, form.experienceLevel)],
                  ["训练目标", labelOf(GOAL_OPTIONS, form.goal)],
                  ["可用器材", labelOf(EQUIPMENT_OPTIONS, form.equipment)],
                  ["单次时长", labelOf(DURATION_OPTIONS, form.sessionDuration)],
                  ["每周天数", `${form.trainingDaysPerWeek} 天`],
                  ["训练风格", labelOf(STYLE_OPTIONS, form.trainingStyle)],
                ].map(([k, v]) => (
                  <div key={k}>
                    <div style={{ fontSize: "11px", color: "var(--text-muted)", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: "3px" }}>{k}</div>
                    <div style={{ fontSize: "14px", fontWeight: 600, color: "var(--text)" }}>{v}</div>
                  </div>
                ))}
                <div style={{ gridColumn: "1/-1" }}>
                  <div style={{ fontSize: "11px", color: "var(--text-muted)", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: "3px" }}>重点部位</div>
                  <div style={{ fontSize: "14px", fontWeight: 600, color: "var(--text)" }}>
                    {form.focusMuscles.map((v) => labelOf(MUSCLE_OPTIONS, v)).join("、")}
                  </div>
                </div>
              </div>
              <div>
                <label style={{ fontSize: "13px", fontWeight: 500, color: "var(--text)", display: "block", marginBottom: "8px" }}>
                  受伤 / 规避说明 <span style={{ color: "var(--text-light)", fontWeight: 400 }}>（选填）</span>
                </label>
                <textarea rows={3} placeholder="如：左膝有伤，避免深蹲..." value={form.injuryNotes}
                  onChange={(e) => setForm({ ...form, injuryNotes: e.target.value })}
                  style={{ resize: "vertical" }} />
              </div>
              {error && (
                <div style={{ background: "var(--danger-bg)", border: "1.5px solid var(--danger-border)", color: "var(--danger)", borderRadius: "10px", padding: "11px 14px", fontSize: "13px" }}>
                  {error}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Nav buttons */}
        <div style={{ display: "flex", justifyContent: "space-between", marginTop: "20px" }}>
          <button onClick={() => step > 0 ? setStep(step - 1) : navigate("/")} style={{
            background: "var(--surface)", border: "1.5px solid var(--border)",
            color: "var(--text-muted)", borderRadius: "10px", padding: "11px 22px", fontSize: "14px", fontWeight: 500,
          }}>
            {step === 0 ? "取消" : "上一步"}
          </button>

          {step < 4 ? (
            <button onClick={() => setStep(step + 1)} disabled={!canNext()} style={{
              background: canNext() ? "var(--accent)" : "var(--border)",
              color: canNext() ? "#fff" : "var(--text-muted)",
              border: "none", borderRadius: "10px", padding: "11px 26px",
              fontSize: "14px", fontWeight: 600,
              cursor: canNext() ? "pointer" : "default",
            }}>
              下一步
            </button>
          ) : (
            <button onClick={handleSubmit} disabled={loading} style={{
              background: loading ? "var(--border-strong)" : "var(--accent)",
              color: "#fff", border: "none", borderRadius: "10px",
              padding: "11px 26px", fontSize: "14px", fontWeight: 600,
              opacity: loading ? 0.75 : 1,
            }}>
              {loading ? "提交中..." : "生成计划 →"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
