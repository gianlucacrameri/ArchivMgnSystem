import { useEffect, useRef, useState } from "react";
import AuthenticatedImage from "./AuthenticatedImage";

export interface GalleryPhoto {
  src?: string | null;
  alt: string;
  /** Kurze Beschriftung, z.B. "Material" oder "Lagerort". */
  label: string;
}

interface Props {
  photos: GalleryPhoto[];
}

/**
 * Fotos nebeneinander zum Durchblättern: auf dem Handy mit dem Finger
 * wischen, am Computer mit den Pfeilen oder den Vorschaubildern darunter.
 * Das Wischen übernimmt der Browser selbst (Scroll-Snap), dadurch fühlt es
 * sich an wie in einer App und braucht keine zusätzliche Bibliothek.
 */
export default function PhotoGallery({ photos }: Props) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);
  const count = photos.length;

  // Welches Foto ist gerade zu sehen? Aus der Scroll-Position abgeleitet,
  // damit Wischen, Pfeile und Vorschaubilder denselben Stand zeigen.
  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;
    const onScroll = () => {
      if (track.clientWidth > 0) setActive(Math.round(track.scrollLeft / track.clientWidth));
    };
    track.addEventListener("scroll", onScroll, { passive: true });
    return () => track.removeEventListener("scroll", onScroll);
  }, []);

  // Ändert sich die Anzahl (Foto ersetzt oder entfernt), wieder vorne anfangen.
  useEffect(() => {
    setActive(0);
    trackRef.current?.scrollTo({ left: 0 });
  }, [count]);

  function goTo(index: number) {
    const track = trackRef.current;
    if (!track) return;
    const target = Math.max(0, Math.min(count - 1, index));
    track.scrollTo({ left: target * track.clientWidth, behavior: "smooth" });
  }

  if (count === 0) {
    return (
      <div className="gallery">
        <div className="gallery__track">
          <div className="gallery__slide">
            <AuthenticatedImage src={null} alt="" size="medium" className="gallery__img" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="gallery">
      <div className="gallery__stage">
        <div
          className="gallery__track"
          ref={trackRef}
          tabIndex={count > 1 ? 0 : undefined}
          onKeyDown={(e) => {
            if (e.key === "ArrowRight") goTo(active + 1);
            if (e.key === "ArrowLeft") goTo(active - 1);
          }}
          aria-label="Fotos"
        >
          {photos.map((photo, index) => (
            <div className="gallery__slide" key={`${photo.src ?? "none"}-${index}`}>
              <AuthenticatedImage src={photo.src} alt={photo.alt} size="medium" className="gallery__img" />
            </div>
          ))}
        </div>

        {count > 1 && (
          <>
            <span className="gallery__label">
              {photos[active]?.label} · {active + 1}/{count}
            </span>
            <button
              type="button"
              className="gallery__arrow gallery__arrow--prev"
              onClick={() => goTo(active - 1)}
              disabled={active === 0}
              aria-label="Vorheriges Foto"
            >
              ‹
            </button>
            <button
              type="button"
              className="gallery__arrow gallery__arrow--next"
              onClick={() => goTo(active + 1)}
              disabled={active === count - 1}
              aria-label="Nächstes Foto"
            >
              ›
            </button>
          </>
        )}
      </div>

      {count > 1 && (
        <div className="gallery__thumbs">
          {photos.map((photo, index) => (
            <button
              type="button"
              key={`${photo.src ?? "none"}-${index}`}
              className={`gallery__thumb ${index === active ? "is-active" : ""}`}
              onClick={() => goTo(index)}
              aria-label={`${photo.label} anzeigen`}
              aria-current={index === active}
            >
              <AuthenticatedImage src={photo.src} alt="" size="thumb" className="gallery__thumb-img" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/** Die Fotos eines Materials in fester Reihenfolge: zuerst das Material, dann der Lagerort. */
export function itemPhotos(item: { name: string; imageUrl?: string; locationImageUrl?: string }): GalleryPhoto[] {
  const photos: GalleryPhoto[] = [];
  if (item.imageUrl) photos.push({ src: item.imageUrl, alt: item.name, label: "Material" });
  if (item.locationImageUrl) {
    photos.push({ src: item.locationImageUrl, alt: `Lagerort von ${item.name}`, label: "Lagerort" });
  }
  return photos;
}
