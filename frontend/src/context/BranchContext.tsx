import React, { createContext, useContext, useState, useEffect } from 'react';
import { CompanyBranchSummary } from '../types/companyBranches';
import { companyBranchesApi } from '../api/companyBranchesApi';

interface BranchContextType {
  activeBranchId: string | null;
  activeBranch: CompanyBranchSummary | null;
  branches: CompanyBranchSummary[];
  loading: boolean;
  setActiveBranchId: (id: string) => void;
  refreshBranches: () => Promise<void>;
}

const BranchContext = createContext<BranchContextType | undefined>(undefined);

export const BranchProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [branches, setBranches] = useState<CompanyBranchSummary[]>([]);
  const [activeBranchId, setActiveBranchIdState] = useState<string | null>(() => {
    return localStorage.getItem('opencontrol_active_branch');
  });
  const [loading, setLoading] = useState(false);

  const refreshBranches = async () => {
    setLoading(true);
    try {
      const res = await companyBranchesApi.getUserBranches();
      setBranches(res.branches);

      // Determine active branch
      const saved = localStorage.getItem('opencontrol_active_branch');
      if (saved && res.branches.some((b) => b.id === saved)) {
        setActiveBranchIdState(saved);
      } else if (res.activeBranchId) {
        setActiveBranchIdState(res.activeBranchId);
        localStorage.setItem('opencontrol_active_branch', res.activeBranchId);
      } else if (res.branches.length > 0) {
        setActiveBranchIdState(res.branches[0].id);
        localStorage.setItem('opencontrol_active_branch', res.branches[0].id);
      }
    } catch (err) {
      console.warn('Could not load user branches', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const token = localStorage.getItem('opencontrol_token');
    if (token) {
      refreshBranches();
    }
  }, []);

  const setActiveBranchId = (id: string) => {
    setActiveBranchIdState(id);
    localStorage.setItem('opencontrol_active_branch', id);
  };

  const activeBranch = branches.find((b) => b.id === activeBranchId) || null;

  return (
    <BranchContext.Provider
      value={{
        activeBranchId,
        activeBranch,
        branches,
        loading,
        setActiveBranchId,
        refreshBranches,
      }}
    >
      {children}
    </BranchContext.Provider>
  );
};

export const useBranch = (): BranchContextType => {
  const context = useContext(BranchContext);
  if (!context) {
    throw new Error('useBranch must be used within a BranchProvider');
  }
  return context;
};
