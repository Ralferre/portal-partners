export function isValidEmail(email: string): boolean {

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export function onlyDigits(value: string): string {
    return (value ?? "").replace(/\D/g, "");
}

export function formatCpf(value: string): string {
    const digits = onlyDigits(value).slice(0, 11);
    const p1 = digits.slice(0, 3);
    const p2 = digits.slice(3, 6);
    const p3 = digits.slice(6, 9);
    const p4 = digits.slice(9, 11);
    let out = p1;
    if (p2) out += `.${p2}`;
    if (p3) out += `.${p3}`;
    if (p4) out += `-${p4}`;
    return out;
}

export function formatCnpj(value: string): string {
    const digits = onlyDigits(value).slice(0, 14);
    const p1 = digits.slice(0, 2);
    const p2 = digits.slice(2, 5);
    const p3 = digits.slice(5, 8);
    const p4 = digits.slice(8, 12);
    const p5 = digits.slice(12, 14);
    let out = p1;
    if (p2) out += `.${p2}`;
    if (p3) out += `.${p3}`;
    if (p4) out += `/${p4}`;
    if (p5) out += `-${p5}`;
    return out;
}

export function isValidCpf(value: string): boolean {
    const cpf = onlyDigits(value);
    if (cpf.length !== 11) return false;
    if (/^(\d)\1{10}$/.test(cpf)) return false;

    const calcDigit = (base: string, factor: number) => {
        let total = 0;
        for (let i = 0; i < base.length; i++) {
            total += Number(base[i]) * (factor - i);
        }
        const mod = total % 11;
        return mod < 2 ? 0 : 11 - mod;
    };

    const d1 = calcDigit(cpf.slice(0, 9), 10);
    const d2 = calcDigit(cpf.slice(0, 10), 11);
    return cpf === cpf.slice(0, 9) + String(d1) + String(d2);
}

export function isValidCnpj(value: string): boolean {
    const cnpj = onlyDigits(value);
    if (cnpj.length !== 14) return false;
    if (/^(\d)\1{13}$/.test(cnpj)) return false;

    const calcDigit = (base: string, weights: number[]) => {
        let total = 0;
        for (let i = 0; i < weights.length; i++) {
            total += Number(base[i]) * weights[i];
        }
        const mod = total % 11;
        return mod < 2 ? 0 : 11 - mod;
    };

    const w1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const w2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

    const d1 = calcDigit(cnpj.slice(0, 12), w1);
    const d2 = calcDigit(cnpj.slice(0, 12) + String(d1), w2);
    return cnpj === cnpj.slice(0, 12) + String(d1) + String(d2);
}