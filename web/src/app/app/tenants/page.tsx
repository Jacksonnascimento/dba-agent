"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import { Building2, Plus, Edit, Check, X } from "lucide-react";
import { toast } from "sonner";

type Tenant = {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
};

export default function TenantsPage() {
  const { isAdmin } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Create form
  const [formName, setFormName] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  // Edit state
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editActive, setEditActive] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  async function fetchTenants() {
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetch<Tenant[]>("/tenants");
      setTenants(data);
    } catch (e: any) {
      setError(e?.message || "Falha ao carregar empresas");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (isAdmin) fetchTenants();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setIsCreating(true);
    try {
      await apiFetch("/tenants", {
        method: "POST",
        body: JSON.stringify({ name: formName }),
      });
      setFormName("");
      toast.success("Empresa criada com sucesso!");
      fetchTenants();
    } catch (e: any) {
      toast.error(e?.message || "Falha ao criar empresa");
    } finally {
      setIsCreating(false);
    }
  }

  function startEdit(t: Tenant) {
    setEditingId(t.id);
    setEditName(t.name);
    setEditActive(t.active);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditName("");
  }

  async function handleSaveEdit(id: number) {
    setIsSaving(true);
    try {
      await apiFetch(`/tenants/${id}`, {
        method: "PUT",
        body: JSON.stringify({ name: editName, active: editActive }),
      });
      toast.success("Empresa atualizada!");
      setEditingId(null);
      fetchTenants();
    } catch (e: any) {
      toast.error(e?.message || "Falha ao atualizar empresa");
    } finally {
      setIsSaving(false);
    }
  }

  if (!isAdmin) {
    return (
      <div className="p-4 text-zinc-400 text-sm">
        Acesso restrito a administradores.
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-white flex items-center gap-2">
          <Building2 className="w-6 h-6 text-blue-500" />
          Gerenciamento de Empresas (Tenants)
        </h1>
        <p className="text-sm text-zinc-400 mt-1">
          Cadastre e gerencie as empresas clientes da plataforma. Cada empresa possui usuários, agentes e bancos de dados isolados.
        </p>
      </div>

      {error && (
        <div className="rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-200">
          {error}
        </div>
      )}

      {/* Create form */}
      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-6">
        <h2 className="text-base font-medium text-white flex items-center gap-2 mb-4">
          <Plus className="w-4 h-4 text-blue-400" />
          Nova Empresa
        </h2>
        <form onSubmit={handleCreate} className="flex gap-3 items-end">
          <div className="flex-1 space-y-1">
            <label className="text-xs text-zinc-400">Nome da Empresa</label>
            <input
              className="w-full rounded-lg bg-zinc-950 border border-zinc-800 px-4 py-2.5 text-sm text-white outline-none focus:ring-2 focus:ring-blue-500/50"
              placeholder="Ex: Acme Corp, TI Solutions..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              required
            />
          </div>
          <button
            type="submit"
            disabled={isCreating}
            className="px-5 py-2.5 bg-blue-500 hover:bg-blue-600 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
          >
            {isCreating ? "Criando..." : "Criar Empresa"}
          </button>
        </form>
      </section>

      {/* List */}
      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/30 p-4">
        <div className="text-sm font-medium text-white mb-3">Empresas Cadastradas</div>
        <div className="overflow-hidden rounded-xl border border-zinc-800">
          <table className="w-full text-sm">
            <thead className="bg-zinc-950/60 text-zinc-300">
              <tr>
                <th className="text-left px-4 py-3 font-medium">ID</th>
                <th className="text-left px-4 py-3 font-medium">Nome</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                <th className="text-left px-4 py-3 font-medium">Criado em</th>
                <th className="text-right px-4 py-3 font-medium">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-4 py-4 text-center text-zinc-500">
                    Carregando...
                  </td>
                </tr>
              ) : tenants.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-4 text-center text-zinc-500">
                    Nenhuma empresa cadastrada.
                  </td>
                </tr>
              ) : (
                tenants.map((t) => (
                  <tr key={t.id} className="hover:bg-zinc-800/20 transition-colors">
                    <td className="px-4 py-3 text-zinc-500">#{t.id}</td>
                    <td className="px-4 py-3 text-white font-medium">
                      {editingId === t.id ? (
                        <input
                          className="rounded bg-zinc-950 border border-zinc-700 px-2 py-1 text-sm text-white outline-none focus:ring-1 focus:ring-blue-500 w-full max-w-xs"
                          value={editName}
                          onChange={(e) => setEditName(e.target.value)}
                        />
                      ) : (
                        t.name
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {editingId === t.id ? (
                        <select
                          className="rounded bg-zinc-950 border border-zinc-700 px-2 py-1 text-sm text-white outline-none"
                          value={editActive ? "true" : "false"}
                          onChange={(e) => setEditActive(e.target.value === "true")}
                        >
                          <option value="true">Ativo</option>
                          <option value="false">Inativo</option>
                        </select>
                      ) : (
                        <span
                          className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full ${
                            t.active
                              ? "bg-green-500/10 text-green-400"
                              : "bg-red-500/10 text-red-400"
                          }`}
                        >
                          {t.active ? "Ativo" : "Inativo"}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-zinc-400 text-xs">
                      {new Date(t.createdAt).toLocaleDateString("pt-BR")}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        {editingId === t.id ? (
                          <>
                            <button
                              onClick={() => handleSaveEdit(t.id)}
                              disabled={isSaving}
                              className="p-1.5 text-green-400 hover:bg-green-500/10 rounded-lg transition-colors disabled:opacity-50"
                              title="Salvar"
                            >
                              <Check className="w-4 h-4" />
                            </button>
                            <button
                              onClick={cancelEdit}
                              className="p-1.5 text-zinc-400 hover:bg-zinc-700/50 rounded-lg transition-colors"
                              title="Cancelar"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </>
                        ) : (
                          <button
                            onClick={() => startEdit(t)}
                            className="p-1.5 text-zinc-400 hover:text-blue-400 hover:bg-blue-500/10 rounded-lg transition-colors"
                            title="Editar"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                        )}
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
