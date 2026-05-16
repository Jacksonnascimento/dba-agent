"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import type { DatabaseConnection } from "@/lib/types";

export default function DatabasesPage() {
  const [items, setItems] = useState<DatabaseConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [active, setActive] = useState<boolean>(true);
  const [name, setName] = useState("");
  const [dbEngine, setDbEngine] = useState("SQL Server");
  const [host, setHost] = useState("");
  const [port, setPort] = useState<number | "">(1433);
  const [database, setDatabase] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [creating, setCreating] = useState(false);

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
    } catch (e: any) {
      setError(e?.message ?? "Falha ao carregar dados");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleCancelEdit() {
    setEditingId(null);
    setName("");
    setDbEngine("SQL Server");
    setHost("");
    setPort(1433);
    setDatabase("");
    setUsername("");
    setPassword("");
    setActive(true);
  }

  function handleEditClick(db: DatabaseConnection) {
    setEditingId(db.id);
    setName(db.name);
    setDbEngine(db.dbEngine);
    setActive(db.active);
    setHost("");
    setPort(db.dbEngine === "PostgreSQL" ? 5432 : 1433);
    setDatabase("");
    setUsername("");
    setPassword("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

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
        <div className="text-sm font-medium">
          {editingId ? `Editando Banco: ${name}` : "Novo banco de dados"}
        </div>
        {editingId && (
          <p className="mt-1 text-xs text-yellow-500">
            Dica: Se não deseja alterar as credenciais ou o host (já salvos e criptografados no servidor), deixe os campos de conexão em branco.
          </p>
        )}

        <form
          className="mt-4 grid gap-4 sm:grid-cols-4"
          onSubmit={async (e) => {
            e.preventDefault();
            setCreating(true);
            setError(null);
            try {
              const payload = {
                name, dbEngine, host, port: port === "" ? null : Number(port), database, username, password, active
              };

              if (editingId) {
                await apiFetch<DatabaseConnection>(`/database-connections/${editingId}`, {
                  method: "PUT",
                  body: JSON.stringify(payload),
                });
                alert("Banco atualizado com sucesso!");
                handleCancelEdit();
              } else {
                await apiFetch<DatabaseConnection>("/database-connections", {
                  method: "POST",
                  body: JSON.stringify(payload),
                });
                handleCancelEdit();
              }

              await refresh();
            } catch (e: any) {
              setError(e?.message ?? "Falha ao salvar banco");
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
          <div className="space-y-1 sm:col-span-1">
            <label className="text-xs text-zinc-400">Engine</label>
            <select
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={dbEngine} onChange={(e) => setDbEngine(e.target.value)}
            >
              <option>SQL Server</option>
              <option>PostgreSQL</option>
            </select>
          </div>

          {editingId && (
            <div className="space-y-1 sm:col-span-1">
              <label className="text-xs text-zinc-400">Status</label>
              <select
                className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
                value={active ? "true" : "false"} onChange={(e) => setActive(e.target.value === "true")}
              >
                <option value="true">Ativo</option>
                <option value="false">Inativo</option>
              </select>
            </div>
          )}

          {/* Linha 2: Host e Porta */}
          <div className="space-y-1 sm:col-span-3">
            <label className="text-xs text-zinc-400">Host / IP</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={host} onChange={(e) => setHost(e.target.value)}
              placeholder="ex: 192.168.1.15 ou localhost" required={!editingId}
            />
          </div>
          <div className="space-y-1 sm:col-span-1">
            <label className="text-xs text-zinc-400">Porta</label>
            <input
              type="number"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={port} onChange={(e) => setPort(e.target.value === "" ? "" : Number(e.target.value))}
              placeholder="ex: 1433" required={!editingId}
            />
          </div>

          {/* Linha 3: Database, User, Pass */}
          <div className="space-y-1 sm:col-span-4">
            <label className="text-xs text-zinc-400">Nome do Banco de Dados (Database)</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={database} onChange={(e) => setDatabase(e.target.value)}
              placeholder="ex: BANCO_SISTEMA" required={!editingId}
            />
          </div>
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Usuário</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={username} onChange={(e) => setUsername(e.target.value)}
              placeholder="ex: admin" required={!editingId}
            />
          </div>
          <div className="space-y-1 sm:col-span-2">
            <label className="text-xs text-zinc-400">Senha (permitido uso de @, #, !)</label>
            <input
              type="password"
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-zinc-600"
              value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="Sua senha segura" required={!editingId}
            />
          </div>

          <div className="sm:col-span-4 flex gap-2 mt-2">
            <button
              className="rounded-lg bg-zinc-50 text-zinc-900 px-4 py-2 text-sm font-medium hover:bg-white disabled:opacity-60"
              disabled={creating} type="submit"
            >
              {creating ? "Salvando..." : (editingId ? "Salvar Banco" : "Criar Banco")}
            </button>
            {editingId && (
              <button
                type="button"
                className="rounded-lg border border-red-900/50 text-red-400 px-4 py-2 text-sm font-medium hover:bg-red-950/40"
                onClick={handleCancelEdit} disabled={creating}
              >
                Cancelar Edição
              </button>
            )}
            {!editingId && (
              <button
                type="button"
                className="rounded-lg border border-zinc-800 px-4 py-2 text-sm text-zinc-200 hover:bg-zinc-950/40"
                onClick={refresh} disabled={loading}
              >
                Recarregar
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium">Bancos cadastrados</div>
        <div className="mt-3 overflow-hidden rounded-xl border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60 text-zinc-300">
              <tr>
                <th className="text-left px-3 py-2 font-medium">Nome</th>
                <th className="text-left px-3 py-2 font-medium">Engine</th>
                <th className="text-left px-3 py-2 font-medium">Ativo</th>
                <th className="text-right px-3 py-2 font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {loading ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={4}>
                    Carregando...
                  </td>
                </tr>
              ) : items.length === 0 ? (
                <tr>
                  <td className="px-3 py-3 text-zinc-400" colSpan={4}>
                    Nenhum banco cadastrado.
                  </td>
                </tr>
              ) : (
                items.map((d) => (
                  <tr key={d.id} className={d.active ? "" : "opacity-50"}>
                    <td className="px-3 py-2">{d.name}</td>
                    <td className="px-3 py-2 text-zinc-300">{d.dbEngine}</td>
                    <td className="px-3 py-2 text-zinc-300">
                      {d.active ? "sim" : "não"}
                    </td>
                    <td className="px-3 py-2 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          className="text-xs text-blue-400 hover:underline"
                          onClick={() => handleEditClick(d)}
                        >
                          Editar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
