/**
 * Verkleinert ein Foto im Browser, bevor es hochgeladen wird.
 *
 * Ein Handyfoto hat 3-8 MB; für das Archiv reichen 2000 Pixel an der längeren
 * Kante (einige hundert KB). Das macht den Upload über Mobilfunk um ein
 * Vielfaches schneller. Klappt die Umwandlung nicht (unbekanntes Format,
 * alter Browser), wird einfach die Originaldatei hochgeladen.
 */
const MAX_EDGE = 2000;
const JPEG_QUALITY = 0.85;
/** Kleine Dateien lohnen den Aufwand nicht und bleiben unverändert. */
const SKIP_BELOW_BYTES = 600 * 1024;

export async function prepareImageForUpload(file: File): Promise<File> {
  // GIF (Animation) und alles Unbekannte bleibt, wie es ist.
  if (!/^image\/(jpeg|png|webp)$/.test(file.type) || file.size <= SKIP_BELOW_BYTES) {
    return file;
  }

  const objectUrl = URL.createObjectURL(file);
  try {
    // Der Browser berücksichtigt beim Dekodieren die Drehung von Handyfotos.
    const image = new Image();
    image.src = objectUrl;
    await image.decode();

    const scale = Math.min(1, MAX_EDGE / Math.max(image.naturalWidth, image.naturalHeight));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));

    const context = canvas.getContext("2d");
    if (!context) return file;
    // JPEG kennt keine Transparenz - durchsichtige Stellen werden weiss.
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", JPEG_QUALITY));
    if (!blob || blob.size >= file.size) return file;

    const name = file.name.replace(/\.[^.]+$/, "") + ".jpg";
    return new File([blob], name, { type: "image/jpeg", lastModified: file.lastModified });
  } catch {
    return file;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}
