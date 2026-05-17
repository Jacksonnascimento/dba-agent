"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";

type Props = {
  dbEngine: string;
  addon: string;
  onAddonChange: (value: string) => void;
  agentWorkerId?: number | null;
  databaseConnectionId?: number | null;
  bankLabel?: string;
};

export function AiPromptFields({
  dbEngine,
  addon,
  onAddonChange,
  agentWorkerId,
  databaseConnectionId,
  bankLabel = "banco",
}: Props) {
  const [contract, setContract] = useState("");
  const [preview, setPreview] = useState("");
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [showContract, setShowContract] = useState(false);
  const [showPreview, setShowPreview] = useState(false);

  useEffect(() => {
    apiFetch<{ contract: string }>(`/ai-prompt/contract?dbEngine=${encodeURIComponent(dbEngine)}`)
      .then((data) => setContract(data.contract))
      .catch(() => setContract("(Não foi possível carregar as regras fixas.)"));
  }, [dbEngine]);

  // Oculta painéis ao trocar de agente, banco ou engine (ex.: editar outro registro)
  useEffect(() => {
    setShowContract(false);
    setShowPreview(false);
    setPreview("");
  }, [dbEngine, agentWorkerId, databaseConnectionId]);

  async function loadPreview() {
    setLoadingPreview(true);
    try {
      const params = new URLSearchParams();
      if (agentWorkerId) params.set("agentWorkerId", String(agentWorkerId));
      if (databaseConnectionId) params.set("databaseConnectionId", String(databaseConnectionId));
      const data = await apiFetch<{ preview: string }>(`/ai-prompt/preview?${params.toString()}`);
      setPreview(data.preview);
      setShowPreview(true);
    } catch {
      setPreview("(Erro ao gerar pré-visualização.)");
      setShowPreview(true);
    } finally {
      setLoadingPreview(false);
    }
  }

  function handlePreviewClick() {
    if (showPreview) {
      setShowPreview(false);
      return;
    }
    void loadPreview();
  }

  const canPreview = Boolean(agentWorkerId || databaseConnectionId);

  return (
    <div className="space-y-4 rounded-xl border border-zinc-800 bg-zinc-950/40 p-4">
      <div>
        <p className="text-sm font-medium text-white">Instruções de IA ({bankLabel})</p>
        <p className="text-xs text-zinc-500 mt-1">
          Complemente o prompt padrão. Regras de segurança, JSON e campos diagnostico / up_script / down_script são fixas.
        </p>
      </div>

      <div className="space-y-2">
        <label className="text-xs text-zinc-400">Suas instruções adicionais</label>
        <textarea
          className="w-full min-h-[120px] rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm text-zinc-100 outline-none focus:ring-2 focus:ring-blue-500/40"
          value={addon}
          onChange={(e) => onAddonChange(e.target.value)}
          placeholder={`Ex.: contexto do ERP, tabelas a evitar, convenções neste ${bankLabel}...`}
          maxLength={4000}
        />
        <p className="text-xs text-zinc-600 text-right">{addon.length}/4000</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          className="text-xs rounded-lg border border-zinc-700 px-3 py-1.5 text-zinc-300 hover:bg-zinc-900"
          onClick={() => setShowContract((v) => !v)}
        >
          {showContract ? "Ocultar" : "Ver"} regras fixas
        </button>
        <button
          type="button"
          className="text-xs rounded-lg border border-zinc-700 px-3 py-1.5 text-zinc-300 hover:bg-zinc-900 disabled:opacity-50"
          onClick={handlePreviewClick}
          disabled={loadingPreview || (!showPreview && !canPreview)}
        >
          {loadingPreview ? "Gerando..." : showPreview ? "Ocultar pré-visualização" : "Pré-visualizar prompt"}
        </button>
        <button
          type="button"
          className="text-xs rounded-lg border border-zinc-700 px-3 py-1.5 text-zinc-300 hover:bg-zinc-900"
          onClick={() => onAddonChange("")}
        >
          Restaurar padrão
        </button>
      </div>

      {showContract ? (
        <pre className="text-xs text-zinc-400 whitespace-pre-wrap max-h-64 overflow-y-auto rounded-lg border border-zinc-800 bg-zinc-950 p-3">
          {contract}
        </pre>
      ) : null}

      {showPreview ? (
        <pre className="text-xs text-zinc-300 whitespace-pre-wrap max-h-80 overflow-y-auto rounded-lg border border-blue-900/40 bg-zinc-950 p-3">
          {preview}
        </pre>
      ) : null}
    </div>
  );
}
