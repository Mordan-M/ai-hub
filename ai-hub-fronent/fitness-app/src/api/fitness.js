import request from "./request";

export const submitPreferences = (data) =>
  request.post("/ai-hub/fitness/preferences", data);

export const getPlan = (planId) =>
  request.get(`/ai-hub/fitness/plans/${planId}`);

export const getLatestPlan = () =>
  request.get("/ai-hub/fitness/plans/latest");

export const regeneratePlan = (planId) =>
  request.post(`/ai-hub/fitness/plans/${planId}/regenerate`);
