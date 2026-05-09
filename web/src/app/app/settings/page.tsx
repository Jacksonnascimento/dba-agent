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

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<TenantSettings>("/tenant/settings");
      setSettings(data);
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
        <h1 className="text-xl font-semibold tracking-tight">Configurações</h1>
        <p className="mt-1 text-sm text-zinc-400">
          BYOK (Bring Your Own Key): a chave da IA fica no tenant, criptografada
          no banco.
        </p>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Tenant</div>
        {loading ? (
          <div className="mt-2 text-sm text-zinc-400">Carregando...</div>
        ) : settings ? (
          <div className="mt-2 text-sm text-zinc-300">
            <div>
              <span className="text-zinc-400">ID:</span> {settings.tenantId}
            </div>
            <div>
              <span className="text-zinc-400">Nome:</span> {settings.tenantName}
            </div>
            <div>
              <span className="text-zinc-400">Gemini:</span>{" "}
              {settings.geminiApiKeyConfigured
                ? `configurada (${settings.geminiApiKeyMasked})`
                : "não configurada"}
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
        <div className="text-sm font-medium">Chave Gemini (BYOK)</div>
        <p className="mt-1 text-xs text-zinc-400">
          Cole sua chave. O backend armazena criptografado e nunca devolve a
          chave completa.
        </p>

        <form
          className="mt-3 space-y-3"
          onSubmit={async (e) => {
            e.preventDefault();
            setSaving(true);
            setError(null);
            try {
              const updated = await apiFetch<TenantSettings>(
                "/tenant/settings/gemini-key",
                {
                  method: "PUT",
                  body: JSON.stringify({ geminiApiKey }),
                }
              );
              setSettings(updated);
              setGeminiApiKey("");
            } catch (e: any) {
              setError(e?.message ?? "Falha ao salvar chave");
            } finally {
              setSaving(false);
            }
          }}
        >
          <input
            className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
            value={geminiApiKey}
            onChange={(e) => setGeminiApiKey(e.target.value)}
            placeholder="AIza..."
            required
          />
          <button
            className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
            disabled={saving}
            type="submit"
          >
            {saving ? "Salvando..." : "Salvar"}
          </button>
        </form>
      </section>
    </div>
  );
}

