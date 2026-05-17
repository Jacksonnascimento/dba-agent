export type LoginResponse = { token: string; role: string; name: string; email: string; };

export type DatabaseConnection = {
  id: number;
  name: string;
  dbEngine: string;
  active: boolean;
  aiInstructionsAddon?: string | null;
  createdAt: string;
};

export type TenantSettings = {
  tenantId: number;
  tenantName: string;
  aiProvider: string;
  aiModel: string | null;
  geminiApiKeyConfigured: boolean;
  geminiApiKeyMasked: string | null;
  claudeApiKeyConfigured: boolean;
  claudeApiKeyMasked: string | null;
};

export type Suggestion = {
  id: number;
  databaseConnectionId: number;
  databaseConnectionName: string;
  databaseName: string;
  tableName: string | null;
  suggestionText: string;
  upScript: string;
  downScript: string;
  status: string;
  createdAt: string;
};

export type AgentTokenCreated = {
  id: number;
  token: string;
  description: string;
  databaseConnectionId: number;
  createdAt: string;
};

export type AgentTokenResponse = {
  id: number;
  description: string;
  databaseConnectionId: number;
  databaseConnectionName: string;
  tokenSuffix: string;
  createdAt: string;
};

export type AgentTask = {
  id: number;
  databaseConnectionId: number;
  databaseName: string;
  schemaHash: string;
  diagnosis: string;
  upScript: string;
  downScript: string;
  status: string;
};

export type Snapshot = {
  id: number;
  schemaHash: string;
  contextHash: string;
  dbEngine: string;
  dmvStats: string | null;
  waitStats: string | null;
  topQueries: string | null;
  executionPlans: string | null;
  indexStats: string | null;
  collectedAt: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export type AuditLog = {
  id: number;
  suggestionId: number;
  databaseConnectionId: number;
  databaseConnectionName: string;
  action: string;
  actorType: string;
  actorIdentifier: string | null;
  details: string | null;
  createdAt: string;
};

