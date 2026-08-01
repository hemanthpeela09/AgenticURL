import { Select, MenuItem } from "@mui/material";
import { useTranslation } from "react-i18next";
import { LANGUAGES, changeLanguage } from "../i18n/index.js";

export default function LanguageSelector() {
    const { i18n } = useTranslation();

    const handleLanguageChange = (event) => {
        changeLanguage(event.target.value);
    };

    return (
        <Select value={i18n.language} onChange={handleLanguageChange}
        sx={{ color: 'white', borderColor: 'white', '& .MuiSvgIcon-root': { color: 'white' } }}>
            {LANGUAGES.map((lang) => (
                <MenuItem key={lang.code} value={lang.code}>
                    {lang.name}
                </MenuItem>
            ))}
        </Select>
    );
}