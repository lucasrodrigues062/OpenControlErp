import React, { createContext, useContext, useState, useEffect } from 'react';
import { SupportedLanguage, TranslationDictionary } from '../types/i18n';
import { enUS } from '../locales/en-US';
import { ptBR } from '../locales/pt-BR';

const dictionaries: Record<SupportedLanguage, TranslationDictionary> = {
  'en-US': enUS,
  'pt-BR': ptBR,
};

export interface I18nContextType {
  currentLanguage: SupportedLanguage;
  setLanguage: (lang: SupportedLanguage) => void;
  t: (
    keyOrNamespace: string,
    keyOrParams?: string | Record<string, string | number>,
    params?: Record<string, string | number>
  ) => string;
  formatDate: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string;
  formatCurrency: (amount: number) => string;
  formatNumber: (amount: number) => string;
}

const I18nContext = createContext<I18nContextType | undefined>(undefined);

export const I18nProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentLanguage, setCurrentLanguageState] = useState<SupportedLanguage>(() => {
    const saved = localStorage.getItem('opencontrol_language');
    if (saved === 'en-US' || saved === 'pt-BR') {
      return saved;
    }
    const browserLang = typeof navigator !== 'undefined' ? navigator.language : 'en-US';
    return browserLang.toLowerCase().startsWith('pt') ? 'pt-BR' : 'en-US';
  });

  const setLanguage = (lang: SupportedLanguage) => {
    setCurrentLanguageState(lang);
    localStorage.setItem('opencontrol_language', lang);
  };

  useEffect(() => {
    document.documentElement.lang = currentLanguage;
  }, [currentLanguage]);

  const t = (
    keyOrNamespace: string,
    keyOrParams?: string | Record<string, string | number>,
    params?: Record<string, string | number>
  ): string => {
    let namespace: keyof TranslationDictionary;
    let key: string;
    let actualParams = params;

    if (typeof keyOrParams === 'string') {
      namespace = keyOrNamespace as keyof TranslationDictionary;
      key = keyOrParams;
    } else {
      const parts = keyOrNamespace.split('.');
      namespace = parts[0] as keyof TranslationDictionary;
      key = parts[1];
      actualParams = keyOrParams;
    }

    const dict = dictionaries[currentLanguage];
    const fallbackDict = dictionaries['en-US'];

    const targetNs = (dict?.[namespace] as unknown) as Record<string, string> | undefined;
    const fallbackNs = (fallbackDict?.[namespace] as unknown) as Record<string, string> | undefined;

    let text = targetNs?.[key] ?? fallbackNs?.[key] ?? keyOrNamespace;

    if (actualParams) {
      Object.entries(actualParams).forEach(([paramKey, val]) => {
        text = text.replace(new RegExp(`\\{${paramKey}\\}`, 'g'), String(val));
      });
    }

    return text;
  };

  const formatDate = (dateInput: Date | string | number, options?: Intl.DateTimeFormatOptions): string => {
    if (!dateInput) return '';
    const date = typeof dateInput === 'string' || typeof dateInput === 'number' ? new Date(dateInput) : dateInput;
    if (isNaN(date.getTime())) return String(dateInput);

    const defaultOptions: Intl.DateTimeFormatOptions = options ?? {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    };

    return new Intl.DateTimeFormat(currentLanguage, defaultOptions).format(date);
  };

  const formatCurrency = (amount: number): string => {
    const currency = currentLanguage === 'pt-BR' ? 'BRL' : 'USD';
    return new Intl.NumberFormat(currentLanguage, {
      style: 'currency',
      currency,
    }).format(amount);
  };

  const formatNumber = (amount: number): string => {
    return new Intl.NumberFormat(currentLanguage).format(amount);
  };

  return (
    <I18nContext.Provider
      value={{
        currentLanguage,
        setLanguage,
        t,
        formatDate,
        formatCurrency,
        formatNumber,
      }}
    >
      {children}
    </I18nContext.Provider>
  );
};

export const useI18n = (): I18nContextType => {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within an I18nProvider');
  }
  return context;
};
