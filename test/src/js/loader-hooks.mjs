export async function resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('rapidcontext/')) {
        let url = `../../../src/plugin/system/files/js/${specifier}`;
        specifier = new URL(url, import.meta.url).href;
    }
    return await nextResolve(specifier, context);
}
