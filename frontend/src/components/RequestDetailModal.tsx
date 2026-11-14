import { useEffect, useState, type FormEvent } from "react";
import type { CategoryDTO, PhotoTarget, StockPrecision, TransactionDTO } from "../types";
import NumberField from "./NumberField";
import { fetchCategories } from "../api/categories";
import Modal from "./Modal";
import AuthenticatedImage from "./AuthenticatedImage";
import { editTransaction, fetchTransaction } from "../api/transactions";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { describeRequest, formatDateTime } from "../utils/describeRequest";

interface Props {
  /** Der Antrag aus der Liste; die vollständigen Angaben werden nachgeladen. */
  request: TransactionDTO;
  processing: boolean;
  onDecide: (status: "COMPLETED" | "CANCELED") => void;
  /** Der Antrag wurde bearbeitet - die Liste dahinter soll die neuen Angaben zeigen. */
  onEdited: (updated: TransactionDTO) => void;
  onClose: () => void;
}

/** Alle Angaben zu einem Antrag, damit ein Admin ihn beurteilen kann. */
export default function RequestDetailModal({ request, processing, onDecide, onEdited, onClose }: Props) {
  const { showError, showSuccess } = useToast();
  const [detail, setDetail] = useState<TransactionDTO>(request);

  useEffect(() => {
    let cancelled = false;
    fetchTransaction(request.id)
      .then((loaded) => {
        if (!cancelled) setDetail(loaded);
      })
      .catch((err) => showError(err instanceof ApiError ? err.message : "Antrag konnte nicht geladen werden"));
    return () => {
      cancelled = true;
    };
  }, [request.id, showError]);

  // ---- Bearbeiten durch den Admin
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [editQuantity, setEditQuantity] = useState<number | "">(0);
  const [editName, setEditName] = useState("");
  const [editPlace, setEditPlace] = useState("");
  const [editCategoryId, setEditCategoryId] = useState<number | "">("");
  const [editPrecision, setEditPrecision] = useState<StockPrecision>("EXACT");
  const [editDescription, setEditDescription] = useState("");
  const [editPhotoTarget, setEditPhotoTarget] = useState<PhotoTarget>("ITEM");

  async function startEditing() {
    setEditQuantity(detail.totalItems);
    setEditName(detail.subjectName ?? "");
    setEditPlace(detail.proposedPlace ?? "");
    setEditCategoryId(detail.proposedCategoryId ?? "");
    setEditPrecision(detail.proposedStockPrecision ?? "EXACT");
    setEditDescription(detail.proposedDescription ?? "");
    setEditPhotoTarget(detail.photoTarget ?? "ITEM");
    if (detail.transactionType === "NEW") {
      try {
        setCategories(await fetchCategories());
      } catch (err) {
        showError(err instanceof ApiError ? err.message : "Kategorien konnten nicht geladen werden");
        return;
      }
    }
    setEditing(true);
  }

  /** Speichert die Anpassungen; mit approve=true wird der Antrag danach gleich freigegeben. */
  async function saveEdit(approve: boolean) {
    setSaving(true);
    try {
      const isNewItem = detail.transactionType === "NEW";
      await editTransaction(detail.id, {
        quantity: detail.transactionType === "PHOTO" ? undefined : editQuantity === "" ? undefined : editQuantity,
        photoTarget: detail.proposedImageUrl && !isNewItem ? editPhotoTarget : undefined,
        ...(isNewItem
          ? {
              name: editName.trim(),
              place: editPlace.trim(),
              categoryId: editCategoryId === "" ? undefined : editCategoryId,
              stockPrecision: editPrecision,
              description: editDescription.trim(),
            }
          : {}),
      });
      const updated = await fetchTransaction(detail.id);
      setDetail(updated);
      onEdited(updated);
      setEditing(false);
      if (approve) {
        onDecide("COMPLETED");
      } else {
        showSuccess("Antrag angepasst");
      }
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Speichern fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  }

  function handleEditSubmit(e: FormEvent) {
    e.preventDefault();
    saveEdit(true);
  }

  const item = detail.item;
  const isNew = detail.transactionType === "NEW";
  const isDelete = detail.transactionType === "DELETE";
  const isPhoto = detail.transactionType === "PHOTO";
  // Auch ein Bestandsantrag kann ein neues Foto mitbringen.
  const hasPhoto = !!detail.proposedImageUrl && !isNew;
  const isLocation = detail.photoTarget === "LOCATION";
  const currentImageUrl = isLocation ? item?.locationImageUrl : item?.imageUrl;
  const stock = item?.stockQuantity ?? 0;
  const stockAfter = detail.transactionType === "ADD" ? stock + detail.totalItems : stock - detail.totalItems;
  const isPending = detail.transactionStatus === "PENDING";
  const changesStock = detail.transactionType === "ADD" || detail.transactionType === "USE";

  return (
    <Modal title={describeRequest(detail)} onClose={onClose}>
      <div className="stacked-form">
        {hasPhoto && (
          <div className="photo-compare">
            <figure>
              <figcaption>Bisher</figcaption>
              <div className="item-detail__image">
                <AuthenticatedImage
                  src={currentImageUrl}
                  alt="Bisheriges Foto"
                  size="medium"
                  className="item-detail__img"
                />
              </div>
            </figure>
            <figure>
              <figcaption>Vorschlag</figcaption>
              <div className="item-detail__image">
                <AuthenticatedImage
                  src={detail.proposedImageUrl}
                  alt="Vorgeschlagenes Foto"
                  size="medium"
                  className="item-detail__img"
                />
              </div>
            </figure>
          </div>
        )}

        {isNew && detail.proposedImageUrl && (
          <div className="item-detail__image">
            <AuthenticatedImage
              src={detail.proposedImageUrl}
              alt="Foto des vorgeschlagenen Materials"
              size="medium"
              className="item-detail__img"
            />
          </div>
        )}

        {isDelete && isPending && (
          <p className="form-error" role="alert">
            Mit der Freigabe wird dieses Material endgültig gelöscht – samt Fotos, offenen Anträgen und
            Reservierungen für Events.
          </p>
        )}

        {!hasPhoto && !isNew && item?.imageUrl && (
          <div className="item-detail__image">
            <AuthenticatedImage src={item.imageUrl} alt={item.name} size="medium" className="item-detail__img" />
          </div>
        )}

        {editing && (
          <form onSubmit={handleEditSubmit} className="stacked-form">
            {isNew && (
              <>
                <label className="field">
                  <span>Name</span>
                  <input required maxLength={255} value={editName} onChange={(e) => setEditName(e.target.value)} />
                </label>
                <label className="field">
                  <span>Lagerort</span>
                  <input required maxLength={255} value={editPlace} onChange={(e) => setEditPlace(e.target.value)} />
                </label>
                <label className="field">
                  <span>Kategorie</span>
                  <select required value={editCategoryId} onChange={(e) => setEditCategoryId(Number(e.target.value))}>
                    <option value="" disabled>
                      Bitte wählen…
                    </option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            )}
            {(isNew || changesStock) && (
              <label className="field">
                <span>{isNew ? "Bestand" : detail.transactionType === "ADD" ? "Menge (hinzufügen)" : "Menge (verbrauchen)"}</span>
                <NumberField
                  value={editQuantity}
                  onChange={setEditQuantity}
                  min={isNew ? 0 : 1}
                  required
                  fallback={isNew ? 0 : 1}
                />
              </label>
            )}
            {isNew && (
              <>
                <div className="field">
                  <span>Bestand ist...</span>
                  <div className="segmented">
                    <button
                      type="button"
                      className={`segmented__option ${editPrecision === "EXACT" ? "is-active" : ""}`}
                      onClick={() => setEditPrecision("EXACT")}
                    >
                      Genau gezählt
                    </button>
                    <button
                      type="button"
                      className={`segmented__option ${editPrecision === "APPROXIMATE" ? "is-active" : ""}`}
                      onClick={() => setEditPrecision("APPROXIMATE")}
                    >
                      Ungefähr
                    </button>
                  </div>
                </div>
                <label className="field">
                  <span>Beschreibung (optional)</span>
                  <textarea
                    maxLength={255}
                    value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    rows={2}
                  />
                </label>
              </>
            )}
            {hasPhoto && (
              <div className="field">
                <span>Neues Foto ersetzt das Foto vom...</span>
                <div className="segmented">
                  <button
                    type="button"
                    className={`segmented__option ${editPhotoTarget === "ITEM" ? "is-active" : ""}`}
                    onClick={() => setEditPhotoTarget("ITEM")}
                  >
                    Material
                  </button>
                  <button
                    type="button"
                    className={`segmented__option ${editPhotoTarget === "LOCATION" ? "is-active" : ""}`}
                    onClick={() => setEditPhotoTarget("LOCATION")}
                  >
                    Lagerort
                  </button>
                </div>
              </div>
            )}
            <div className="button-row">
              <button type="submit" className="btn btn--primary" disabled={saving || processing}>
                Speichern und freigeben
              </button>
              <button type="button" className="btn btn--secondary" disabled={saving} onClick={() => saveEdit(false)}>
                Nur speichern
              </button>
              <button type="button" className="btn btn--link" disabled={saving} onClick={() => setEditing(false)}>
                Abbrechen
              </button>
            </div>
            <p className="hint">Das Mitglied sieht, dass der Antrag angepasst wurde.</p>
          </form>
        )}

        {!editing && (
        <dl className="item-detail__facts item-detail__facts--2col">
          <div>
            <dt>Material</dt>
            <dd>{item?.name ?? detail.subjectName ?? "Unbekannt"}</dd>
          </div>
          <div>
            <dt>Artikelnummer</dt>
            <dd>{item?.sku ?? (isNew ? "wird bei Freigabe vergeben" : "–")}</dd>
          </div>
          <div>
            <dt>Lagerort</dt>
            <dd>{item?.place ?? detail.proposedPlace ?? (isNew ? "fehlt noch" : "–")}</dd>
          </div>
          {isNew && (
            <>
              <div>
                <dt>Kategorie</dt>
                <dd>{detail.proposedCategoryName ?? "–"}</dd>
              </div>
              <div>
                <dt>Bestand</dt>
                <dd>
                  {detail.totalItems} Stk. ({detail.proposedStockPrecision === "APPROXIMATE" ? "ungefähr" : "genau"})
                </dd>
              </div>
            </>
          )}
          {isDelete && item && (
            <div>
              <dt>Bestand</dt>
              <dd>{item.stockQuantity} Stk.</dd>
            </div>
          )}
          {(isPhoto || hasPhoto) && (
            <div>
              <dt>Neues Foto für</dt>
              <dd>{isLocation ? "Lagerort" : "Material"}</dd>
            </div>
          )}
          {changesStock && (
            <div>
              <dt>Bestand</dt>
              <dd>
                {isPending ? (
                  <>
                    {stock} → {stockAfter} Stk.
                  </>
                ) : (
                  <>{stock} Stk. (aktuell)</>
                )}
              </dd>
            </div>
          )}
          <div>
            <dt>Beantragt von</dt>
            <dd>{detail.user?.username ?? "ehemaligem Mitglied"}</dd>
          </div>
          <div>
            <dt>Beantragt am</dt>
            <dd>{formatDateTime(detail.createdAt)}</dd>
          </div>
          {detail.user?.email && (
            <div>
              <dt>E-Mail</dt>
              <dd>{detail.user.email}</dd>
            </div>
          )}
          {detail.user?.phoneNumber && (
            <div>
              <dt>Telefon</dt>
              <dd>{detail.user.phoneNumber}</dd>
            </div>
          )}
        </dl>
        )}

        {!editing && isNew && isPending && !detail.proposedPlace && (
          <p className="form-error" role="alert">
            Es fehlt noch der Lagerort. Bitte über „Bearbeiten" eintragen, bevor du freigibst.
          </p>
        )}
        {!editing && detail.editedByName && (
          <p className="hint">Von {detail.editedByName} angepasst.</p>
        )}

        {!editing && isNew && detail.proposedDescription && (
          <p className="item-detail__description">Beschreibung: {detail.proposedDescription}</p>
        )}
        {detail.note && (
          <p className="item-detail__description">
            {isDelete ? "Begründung" : "Notiz"}: {detail.note}
          </p>
        )}

        {changesStock && isPending && stockAfter < 0 && (
          <p className="form-error" role="alert">
            Der Bestand reicht für diesen Antrag nicht aus.
          </p>
        )}

        {!isPending && (
          <p className="hint">
            {detail.transactionStatus === "COMPLETED" ? "Freigegeben" : "Abgelehnt"}
            {detail.decidedByName ? ` von ${detail.decidedByName}` : ""}
            {detail.updatedAt ? ` am ${formatDateTime(detail.updatedAt)}` : ""}
          </p>
        )}

        {isPending && !editing && (
          <div className="button-row">
            <button
              type="button"
              className={`btn ${isDelete ? "btn--danger" : "btn--primary"}`}
              disabled={processing}
              onClick={() => onDecide("COMPLETED")}
            >
              {isDelete ? "Löschen freigeben" : isNew ? "Freigeben und anlegen" : "Freigeben"}
            </button>
            <button type="button" className="btn btn--secondary" disabled={processing} onClick={() => onDecide("CANCELED")}>
              Ablehnen
            </button>
            {!isDelete && (
              <button type="button" className="btn btn--link" disabled={processing} onClick={startEditing}>
                Bearbeiten
              </button>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}
