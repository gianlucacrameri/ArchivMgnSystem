import { useEffect, useState } from "react";
import { fetchAuthorizedImage } from "../api/client";
import { IconBox } from "./Icons";

interface Props {
  src?: string | null;
  alt: string;
  className?: string;
  /**
   * Der Server liefert verkleinerte Varianten: "thumb" (~480px, einige 10 KB)
   * für Kacheln und Listen, "medium" (~1600px) für die Detailansicht. Das
   * Original eines Handyfotos hat mehrere MB und wird nie direkt geladen.
   */
  size: "thumb" | "medium";
}

/**
 * Jedes Foto wird nur einmal pro Session geladen und als Blob-URL
 * zwischengespeichert. Ohne diesen Cache lädt jede neue Montage derselben
 * Komponente (z.B. beim erneuten Öffnen eines Items oder Zurücknavigieren)
 * das komplette Bild nochmal über einen authentifizierten Request neu -
 * das war ein spürbarer Teil der gefühlten Langsamkeit der App.
 */
const imageCache = new Map<string, Promise<string>>();

function getCachedImage(src: string): Promise<string> {
  let promise = imageCache.get(src);
  if (!promise) {
    promise = fetchAuthorizedImage(src);
    imageCache.set(src, promise);
    promise.catch(() => imageCache.delete(src));
  }
  return promise;
}

/**
 * Item-Fotos liegen hinter dem Login (JWT im Authorization-Header), ein
 * normales <img src> kann diesen Header nicht mitschicken. Diese Komponente
 * lädt das Bild per fetch und zeigt es als (gecachte) Blob-URL an.
 */
export default function AuthenticatedImage({ src, alt, className, size }: Props) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;

    setFailed(false);
    setObjectUrl(null);

    if (!src) {
      return;
    }

    getCachedImage(`${src}?size=${size}`)
      .then((url) => {
        if (!cancelled) setObjectUrl(url);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });

    return () => {
      cancelled = true;
    };
  }, [src, size]);

  if (!src || failed) {
    return (
      <div className={`image-placeholder ${className ?? ""}`} aria-hidden="true">
        <IconBox className="image-placeholder__icon" />
      </div>
    );
  }

  if (!objectUrl) {
    return <div className={`image-placeholder image-placeholder--loading ${className ?? ""}`} aria-hidden="true" />;
  }

  // Lässt sich die Datei nicht darstellen (defekt oder kein echtes Bild),
  // erscheint der Platzhalter statt des "kaputtes Bild"-Symbols des Browsers.
  return <img src={objectUrl} alt={alt} className={className} loading="lazy" onError={() => setFailed(true)} />;
}
