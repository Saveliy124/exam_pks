package ru.mirea.examcenter.kr3.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import ru.mirea.examcenter.kr3.domain.*;
import ru.mirea.examcenter.kr3.dto.ExamDtos.*;
import ru.mirea.examcenter.kr3.repository.*;
import ru.mirea.examcenter.kr3.security.CurrentUserService;
import ru.mirea.examcenter.kr3.error.NotFoundException;
import java.util.*;
@Service
@Transactional
public class NoteService {
 private final ApplicationNoteRepository notes;private final ApplicationService apps;private final CurrentUserService user;private final AuditService audit;
 public NoteService(ApplicationNoteRepository notes,ApplicationService apps,CurrentUserService user,AuditService audit){this.notes=notes;this.apps=apps;this.user=user;this.audit=audit;}
 @Transactional(readOnly=true) public List<NoteResponse> list(UUID applicationId){apps.require(applicationId);return notes.findByApplicationPublicIdOrderByPinnedDescCreatedAtDesc(applicationId).stream().map(ExamMapper::note).toList();}
 public NoteResponse create(UUID app,NoteRequest r){ApplicationNote n=new ApplicationNote();n.setId(UUID.randomUUID());n.setApplication(apps.require(app));n.setAuthor(user.requireEntity());n.setBody(DirectoryService.text(r.body(),1));n.setPinned(r.pinned());notes.saveAndFlush(n);event("NOTE_CREATED",n);return ExamMapper.note(n);}
 public NoteResponse update(UUID app,UUID id,NoteRequest r){ApplicationNote n=require(app,id);owner(n);DirectoryService.version(r.version(),n.getVersion());n.setBody(DirectoryService.text(r.body(),1));n.setPinned(r.pinned());notes.flush();event("NOTE_UPDATED",n);return ExamMapper.note(n);}
 public void delete(UUID app,UUID id,long v){ApplicationNote n=require(app,id);owner(n);DirectoryService.version(v,n.getVersion());event("NOTE_DELETED",n);notes.delete(n);notes.flush();}
 private ApplicationNote require(UUID app,UUID id){return notes.findById(id).filter(n->n.getApplication().getPublicId().equals(app)).orElseThrow(()->new NotFoundException("Заметка не найдена"));}
 private void owner(ApplicationNote n){var actor=user.requireEntity();if(!actor.getId().equals(n.getAuthor().getId())&&!actor.getRoles().contains(Role.ADMIN))throw new AccessDeniedException("Изменять чужую заметку может только администратор");}
 private void event(String type,ApplicationNote n){audit.record(type,"NOTE",n.getId(),"Заметка к заявке "+n.getApplication().getExternalId(),Map.of("applicationId",n.getApplication().getPublicId(),"pinned",n.getPinned()));}
}
