import request from "./request";

export const submitPreferences = (data) =>
  request.post("/api/v1/fitness/preferences", data);

export const getPlan = (planId) =>
  request.get(`/api/v1/fitness/plans/${planId}`);

export const getLatestPlan = () =>
  request.get("/api/v1/fitness/plans/latest");

export const regeneratePlan = (planId) =>
  request.post(`/api/v1/fitness/plans/${planId}/regenerate`);
