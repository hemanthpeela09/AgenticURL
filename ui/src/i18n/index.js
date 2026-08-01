import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import enUS from "./locales/en-US.json";
import itIT from "./locales/it-IT.json";

export const LANGUAGES = [
    { code: 'en', name: 'English' },
    { code: 'it', name: 'Italiano' },
];

const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('lang') : null;

export const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem('lang', lng);
    }
};

i18n
    .use(initReactI18next)
    .init({
        resources: {
            en: { translation: enUS },
            it: { translation: itIT },
        },
        lng: stored || "en-US",
        fallbackLng: "en-US",
        interpolation: {
            escapeValue: false,
        },
    });
    
export default i18n;