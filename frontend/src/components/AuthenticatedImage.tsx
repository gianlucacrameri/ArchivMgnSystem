import { useEffect, useState } from "react";
import { fetchAuthorizedImage } from "../api/client";
import { IconBox } from "./Icons";

interface Props {
  src?: string | null;
  alt: string;
  className?: string;
}

/**
 * Item-Fotos liegen hinter dem Login (JWT im Authorization-Header), ein
 * normales <img src> kann diesen Header nicht mitschicken. Diese Komponente
 * lädt das Bild per fetch und zeigt es als Blob-URL an.
 */
export default function AuthenticatedImage({ src, alt, className }: Props) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let currentUrl: string | null = null;
    let cancelled = false;

    setFailed(false);
    setObjectUrl(null);

    if (!src) {
      return;
    }

    fetchAuthorizedImage(src)
      .then((url) => {
        if (cancelled) {
          URL.revokeObjectURL(url);
          return;
        }
        currentUrl = url;
        setObjectUrl(url);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });

    return () => {
      cancelled = true;
      if (currentUrl) URL.revokeObjectURL(currentUrl);
    };
  }, [src]);

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

  return <img src={objectUrl} alt={alt} className={className} loading="lazy" />;
}
