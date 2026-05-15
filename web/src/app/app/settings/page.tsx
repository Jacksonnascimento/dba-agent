"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { TenantSettings } from "@/lib/types";

export default function SettingsPage() {
  const [settings, setSettings] = useState<TenantSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [geminiApiKey, setGeminiApiKey] = useState("");
  const [claudeApiKey, setClaudeApiKey] = useState("");
  const [aiProvider, setAiProvider] = useState("GEMINI");
  const [aiModel, setAiModel] = useState("");

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<TenantSettings>("/tenant/settings");
      setSettings(data);
      if (data.aiProvider) setAiProvider(data.aiProvider);
      if (data.aiModel) setAiModel(data.aiModel);
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar configurações");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Configurações de IA</h1>
        <p className="mt-1 text-sm text-zinc-400">
          BYOK (Bring Your Own Key): a chave da IA fica no seu tenant, isolada e criptografada
          com segurança militar no banco de dados.
        </p>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Situação do Tenant</div>
        {loading ? (
          <div className="mt-2 text-sm text-zinc-400">Carregando...</div>
        ) : settings ? (
          <div className="mt-2 text-sm text-zinc-300">
            <div><span className="text-zinc-400">ID:</span> {settings.tenantId}</div>
            <div><span className="text-zinc-400">Nome:</span> {settings.tenantName}</div>
            <div><span className="text-zinc-400">Provedor Ativo:</span> {settings.aiProvider || "GEMINI"}</div>
            <div><span className="text-zinc-400">Modelo Específico:</span> {settings.aiModel || "Não definido (usará o padrão)"}</div>
            <div className="mt-2">
              <span className="text-zinc-400">Chave Gemini:</span>{" "}
              {settings.geminiApiKeyConfigured
                ? <span className="text-green-400">Configurada ({settings.geminiApiKeyMasked})</span>
                : <span className="text-red-400">Não configurada</span>}
            </div>
            <div>
              <span className="text-zinc-400">Chave Claude (Anthropic):</span>{" "}
              {settings.claudeApiKeyConfigured
                ? <span className="text-green-400">Configurada ({settings.claudeApiKeyMasked})</span>
                : <span className="text-red-400">Não configurada</span>}
            </div>
          </div>
        ) : null}

        <button
          className="mt-3 rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
          onClick={load}
          disabled={loading}
        >
          Recarregar
        </button>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Alterar Configurações de IA</div>
        <form
          className="mt-4 grid gap-4 sm:grid-cols-2"
          onSubmit={async (e) => {
            e.preventDefault();
            setSaving(true);
            setError(null);
            try {
              const payload: any = {
                aiProvider,
                aiModel: aiModel.trim() || null
              };
              if (aiProvider === 'GEMINI' && geminiApiKey) payload.geminiApiKey = geminiApiKey;
              if (aiProvider === 'CLAUDE' && claudeApiKey) payload.claudeApiKey = claudeApiKey;

              const updated = await apiFetch<TenantSettings>(
                "/tenant/settings/ai-config",
                {
                  method: "PUT",
                  body: JSON.stringify(payload),
                }
              );
              setSettings(updated);
              setGeminiApiKey("");
              setClaudeApiKey("");
              alert("Configurações atualizadas com sucesso!");
            } catch (e: any) {
              setError(e?.message ?? "Falha ao salvar configurações");
            } finally {
              setSaving(false);
            }
          }}
        >
          <div className="space-y-1 sm:col-span-1">
            <label className="text-xs text-zinc-400">Provedor de IA</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={aiProvider}
              onChange={(e) => setAiProvider(e.target.value)}
            >
              <option value="GEMINI">Google Gemini</option>
              <option value="CLAUDE">Anthropic Claude</option>
            </select>
          </div>

          <div className="space-y-1 sm:col-span-1">
            <label className="text-xs text-zinc-400">Modelo Específico (Opcional)</label>
            <input
              type="text"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600 placeholder:text-zinc-700"
              value={aiModel}
              onChange={(e) => setAiModel(e.target.value)}
              placeholder={aiProvider === 'GEMINI' ? "ex: gemini-2.5-flash" : "ex: claude-3-5-sonnet-20241022"}
            />
          </div>

          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">
              Nova Chave de API ({aiProvider})
            </label>
            <input
              type="password"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600 placeholder:text-zinc-700"
              value={aiProvider === 'GEMINI' ? geminiApiKey : claudeApiKey}
              onChange={(e) => {
                if (aiProvider === 'GEMINI') setGeminiApiKey(e.target.value);
                else setClaudeApiKey(e.target.value);
              }}
              placeholder="Deixe em branco para manter a chave atual criptografada salva no banco"
            />
          </div>

          <div className="sm:col-span-2 flex gap-3">
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={saving}
              type="submit"
            >
              {saving ? "Salvando..." : "Salvar Configurações"}
            </button>
            <button
              className="rounded-lg border border-zinc-800 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-900 disabled:opacity-60"
              disabled={saving}
              type="button"
              onClick={async () => {
                const btn = document.activeElement as HTMLButtonElement;
                if (btn) btn.disabled = true;
                setError(null);
                try {
                  const res = await apiFetch<{message: string}>("/tenant/settings/ai-test", {
                    method: "POST"
                  });
                  alert(res.message || "Conexão bem-sucedida!");
                } catch (e: any) {
                  setError(e?.message ?? "Falha ao testar conexão");
                } finally {
                  if (btn) btn.disabled = false;
                }
              }}
            >
              Testar Conexão Salva
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}

