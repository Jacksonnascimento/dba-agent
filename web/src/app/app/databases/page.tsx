"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { AgentTokenCreated, DatabaseConnection } from "@/lib/types";

export default function DatabasesPage() {
  const [items, setItems] = useState<DatabaseConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Novos states separados para conexão
  const [name, setName] = useState("");
  const [dbEngine, setDbEngine] = useState("SQL Server");
  const [host, setHost] = useState("");
  const [port, setPort] = useState<number | "">(1433);
  const [database, setDatabase] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  
  const [creating, setCreating] = useState(false);

  const [tokenDbId, setTokenDbId] = useState<number | null>(null);
  const [tokenDescription, setTokenDescription] = useState("");
  const [creatingToken, setCreatingToken] = useState(false);
  const [createdToken, setCreatedToken] = useState<AgentTokenCreated | null>(null);

  const activeDbOptions = useMemo(
    () => items.filter((d) => d.active),
    [items]
  );

  // Sugestão de porta automática baseada na engine
  useEffect(() => {
    if (dbEngine === "SQL Server") setPort(1433);
    if (dbEngine === "PostgreSQL") setPort(5432);
  }, [dbEngine]);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<DatabaseConnection[]>("/database-connections");
      setItems(data);
      if (tokenDbId == null && data.length > 0) setTokenDbId(data[0]!.id);
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar bancos");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Bancos</h1>
        <p className="mt-1 text-sm text-zinc-400">
          Cadastre múltiplos bancos por tenant. O sistema cuidará da formatação e criptografia dos dados de acesso.
        </p>
      </div>

      {error ? (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      ) : null}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Novo banco de dados</div>
        
        <form
          className="mt-4 grid gap-4 sm:grid-cols-4"
          onSubmit={async (e) => {
            e.preventDefault();
            setCreating(true);
            setError(null);
            try {
              // Envia o payload com os campos quebrados para o backend resolver
              await apiFetch<DatabaseConnection>("/database-connections", {
                method: "POST",
                body: JSON.stringify({ 
                    name, dbEngine, host, port: Number(port), database, username, password 
                }),
              });
              
              // Limpa form após o sucesso
              setName(""); setHost(""); setDatabase(""); setUsername(""); setPassword("");
              await refresh();
            } catch (e: any) {
              setError(e?.message ?? "Falha ao criar banco");
            } finally {
              setCreating(false);
            }
          }}
        >
          {/* Linha 1: Nome e Engine */}
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Apelido do Banco</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={name} onChange={(e) => setName(e.target.value)}
              placeholder="ex: Produção ERP" required
            />
          </div>
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Engine</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={dbEngine} onChange={(e) => setDbEngine(e.target.value)}
            >
              <option>SQL Server</option>
              <option>PostgreSQL</option>
            </select>
          </div>

          {/* Linha 2: Host e Porta */}
          <div className="space-y-1 sm:col-span-3">
            <label className="text-xs text-zinc-400">Host / IP</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={host} onChange={(e) => setHost(e.target.value)}
              placeholder="ex: 10.158.0.119 ou localhost" required
            />
          </div>
          <div className="space-y-1 sm:col-span-1">
            <label className="text-xs text-zinc-400">Porta</label>
            <input
              type="number"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={port} onChange={(e) => setPort(e.target.value === "" ? "" : Number(e.target.value))}
              placeholder="ex: 1433" required
            />
          </div>

          {/* Linha 3: Database, User, Pass */}
          <div className="space-y-1 sm:col-span-4">
            <label className="text-xs text-zinc-400">Nome do Banco de Dados (Database)</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={database} onChange={(e) => setDatabase(e.target.value)}
              placeholder="ex: FPG_WEB_PM_CAMACARI" required
            />
          </div>
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Usuário</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={username} onChange={(e) => setUsername(e.target.value)}
              placeholder="ex: admin" required
            />
          </div>
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Senha (permitido uso de @, #, !)</label>
            <input
              type="password"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="Sua senha segura" required
            />
          </div>

          <div className="sm:col-span-4 flex gap-2 mt-2">
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={creating} type="submit"
            >
              {creating ? "Criando..." : "Salvar Banco"}
            </button>
            <button
              type="button"
              className="rounded-lg border border-zinc-800 px-4 py-2 text-sm text-zinc-200 hover:bg-zinc-950/40"
              onClick={refresh} disabled={loading}
            >
              Recarregar
            </button>
          </div>
        </form>
      </section>

      {/* ==================================================== */}
      {/* SECÕES RESTANTES (TABELA DE BANCOS E TOKENS) INTACTAS */}
      {/* ==================================================== */}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Bancos cadastrados</div>
        <div className="mt-3 overflow-hidden rounded-xl border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60 text-zinc-300">
              <tr>
                <th className="text-left px-3 py-2 font-medium">Nome</th>
                <th className="text-left px-3 py-2 font-medium">Engine</th>
                <th className="text-left px-3 py-2 font-medium">Ativo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {loading ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={3}>
                    Carregando...
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={3}>
                    Nenhum banco cadastrado.
                  </td>
                </tr>
              ) : (
                items.map((d) => (
                  <tr key={d.id}>
                    <td className="px-3 py-2">{d.name}</td>
                    <td className="px-3 py-2 text-zinc-300">{d.dbEngine}</td>
                    <td className="px-3 py-2 text-zinc-300">
                      {d.active ? "sim" : "não"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Token do agente (por banco)</div>
        <p className="mt-1 text-xs text-zinc-400">
          Gere um token e copie para configurar o worker.
        </p>

        <form
          className="mt-3 grid gap-3 sm:grid-cols-2"
          onSubmit={async (e) => {
            e.preventDefault();
            if (!tokenDbId) return;
            setCreatingToken(true);
            setError(null);
            setCreatedToken(null);
            try {
              const token = await apiFetch<AgentTokenCreated>("/agent-tokens", {
                method: "POST",
                body: JSON.stringify({
                  description: tokenDescription || "agent",
                  databaseConnectionId: tokenDbId,
                }),
              });
              setCreatedToken(token);
              setTokenDescription("");
            } catch (e: any) {
              setError(e?.message ?? "Falha ao criar token");
            } finally {
              setCreatingToken(false);
            }
          }}
        >
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Banco</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={tokenDbId ?? ""}
              onChange={(e) => setTokenDbId(Number(e.target.value))}
              disabled={activeDbOptions.length === 0}
            >
              {activeDbOptions.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-zinc-400">Descrição</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={tokenDescription}
              onChange={(e) => setTokenDescription(e.target.value)}
              placeholder="ex: Servidor SQL Prod Principal"
            />
          </div>

          <div className="sm:col-span-2 flex gap-2">
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={creatingToken || activeDbOptions.length === 0}
              type="submit"
            >
              {creatingToken ? "Gerando..." : "Gerar token"}
            </button>
          </div>
        </form>

        {createdToken ? (
          <div className="mt-4 rounded-xl border border-zinc-800 bg-zinc-950 p-4">
            <div className="text-xs text-zinc-400">Token gerado</div>
            <div className="mt-1 font-mono text-xs break-all">
              {createdToken.token}
            </div>
            <button
              className="mt-3 rounded-lg border border-zinc-800 px-3 py-2 text-xs hover:bg-zinc-900"
              onClick={async () => {
                await navigator.clipboard.writeText(createdToken.token);
              }}
            >
              Copiar
            </button>
          </div>
        ) : null}
      </section>
    </div>
  );
}