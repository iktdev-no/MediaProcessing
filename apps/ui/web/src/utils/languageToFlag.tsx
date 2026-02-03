import ReactCountryFlag from "react-country-flag";

export const iso3ToIso2: Record<string, string> = {
    eng: "GB",
    jpn: "JP",
    nor: "NO",
    nno: "NO",
    nob: "NO",
    swe: "SE",
    dan: "DK",
    fin: "FI",
    deu: "DE",
    fra: "FR",
    spa: "ES",
    ita: "IT",
    pol: "PL",
    rus: "RU",
    kor: "KR",
    zho: "CN",
    chi: "CN",
    ara: "SA",
    por: "PT",
    nld: "NL",
    hun: "HU",
    ces: "CZ",
    slk: "SK"
};

export function LanguageFlag({ lang }: { lang: string }) {
    const code = iso3ToIso2[lang];
    if (!code) return <span>🏳️ {lang} </span>;

    return (
        <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }
        }>
            <ReactCountryFlag
                countryCode={code}
                svg
                style={{ width: "1.5em", height: "1.5em" }}
            />
            {lang}
        </span>
    );
}
