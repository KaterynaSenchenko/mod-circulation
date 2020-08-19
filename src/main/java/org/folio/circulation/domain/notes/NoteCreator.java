package org.folio.circulation.domain.notes;

import java.util.concurrent.CompletableFuture;

import org.folio.circulation.domain.NoteLink;
import org.folio.circulation.infrastructure.storage.notes.NotesRepository;
import org.folio.circulation.support.Result;

public class NoteCreator {
  private final NotesRepository notesRepository;

  public NoteCreator(NotesRepository notesRepository) {
    this.notesRepository = notesRepository;
  }

  public CompletableFuture<Result<Note>> createGeneralUserNote(String userId, String message) {
    final GeneralNoteTypeValidator validator = new GeneralNoteTypeValidator();

    return notesRepository.findGeneralNoteType()
      .thenApply(validator::refuseIfNoteTypeNotFound)
      .thenCompose(r -> r.after(noteType -> notesRepository.create(Note.builder()
        .title(message)
        .typeId(noteType.getId())
        .content(message)
        .domain("loans")
        .link(new NoteLink(userId, NoteLinkType.USER.getValue()))
        .build())));
  }
}
