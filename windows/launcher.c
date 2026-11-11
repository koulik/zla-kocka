#include <windows.h>
#include <wchar.h>

static const wchar_t *TITLE = L"Zl\u00e1 Ko\u010dka";

static int report_error(const wchar_t *text) {
    MessageBoxW(NULL, text, TITLE, MB_OK | MB_ICONERROR);
    return 1;
}

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previous, PWSTR args, int show) {
    (void) instance;
    (void) previous;
    (void) args;
    (void) show;
    HANDLE lock = CreateMutexW(NULL, FALSE, L"Local\\ZlaKocka.CursorMotion");
    if (lock == NULL) {
        return report_error(L"Aplikaci se nepoda\u0159ilo spustit.");
    }
    if (GetLastError() == ERROR_ALREADY_EXISTS) {
        CloseHandle(lock);
        MessageBoxW(NULL, L"Aplikace u\u017e b\u011b\u017e\u00ed. Jej\u00ed ikonu najdete u hodin.",
                TITLE, MB_OK | MB_ICONINFORMATION);
        return 0;
    }

    wchar_t folder[4096];
    wchar_t java[4160];
    wchar_t command[8400];
    DWORD length = GetModuleFileNameW(NULL, folder, ARRAYSIZE(folder));
    if (length == 0 || length >= ARRAYSIZE(folder)) {
        CloseHandle(lock);
        return report_error(L"Nepoda\u0159ilo se naj\u00edt slo\u017eku aplikace.");
    }
    wchar_t *separator = wcsrchr(folder, L'\\');
    if (separator == NULL) {
        CloseHandle(lock);
        return report_error(L"Nepoda\u0159ilo se naj\u00edt slo\u017eku aplikace.");
    }
    *separator = L'\0';
    swprintf(java, ARRAYSIZE(java), L"%ls\\runtime\\bin\\javaw.exe", folder);
    swprintf(command, ARRAYSIZE(command),
            L"\"%ls\" --enable-native-access=ALL-UNNAMED -jar \"%ls\\app\\ZlaKocka.jar\"",
            java, folder);

    STARTUPINFOW startup = {0};
    startup.cb = sizeof(startup);
    PROCESS_INFORMATION process = {0};
    if (!CreateProcessW(java, command, NULL, NULL, FALSE, 0, NULL, NULL, &startup, &process)) {
        CloseHandle(lock);
        return report_error(L"Zl\u00e1 Ko\u010dka se nepoda\u0159ila spustit. Rozbalte cel\u00fd archiv ZIP.");
    }

    CloseHandle(process.hThread);
    WaitForSingleObject(process.hProcess, INFINITE);
    DWORD exit_code = 1;
    GetExitCodeProcess(process.hProcess, &exit_code);
    CloseHandle(process.hProcess);
    CloseHandle(lock);
    if (exit_code != 0) {
        return report_error(L"B\u011bh aplikace byl ukon\u010den chybou.");
    }
    return 0;
}
