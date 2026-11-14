import { useEffect, useState } from "react";
import type { TransactionDTO } from "../types";
import Modal from "./Modal";
import AuthenticatedImage from "./AuthenticatedImage";
import { fetchTransaction } from "../api/transactions";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { describeRequest, formatDateTime } from "../utils/describeRequest";

interface Props {
  /** Der Antrag aus der Liste; die vollständigen Angaben werden nachgeladen. */
  request: TransactionDTO;
  processing: boolean;
  onDecide: (status: "COMPLETED" | "CANCELED") => void;
  onClose: () => void;
}

/** Alle Angaben zu einem Antrag, damit ein Admin ihn beurteilen kann. */
export default function RequestDetailModal({ request, processing, onDecide, onClose }: Props) {
  const { showError } = useToast();
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
            <dd>{item?.place ?? detail.proposedPlace ?? "–"}</dd>
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

        {isNew && detail.proposedDescription && (
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

        {isPending && (
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
          </div>
        )}
      </div>
    </Modal>
  );
}
