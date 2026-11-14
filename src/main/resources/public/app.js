const $ = (id) => document.getElementById(id);
const state = { config:null, exam:null, index:0, result:null, timerId:null, busy:false };
const letters = ['A','B','C','D'];
const isAssessmentTab = new URLSearchParams(window.location.search).get('assessment') === 'active';
if (isAssessmentTab) document.body.classList.add('assessment-mode');

async function api(path, body) {
  const options = { method: body === undefined ? 'GET' : 'POST', headers: {} };
  if (body !== undefined) { options.headers['Content-Type'] = 'application/json'; options.body = JSON.stringify(body); }
  const response = await fetch(path, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || 'The request could not be completed.');
  return data;
}
function showView(id) { document.querySelectorAll('.view').forEach(v => v.classList.toggle('active', v.id === id)); window.scrollTo({top:0,behavior:'smooth'}); }
function toast(message) { const el=$('toast'); el.textContent=message; el.classList.add('show'); setTimeout(()=>el.classList.remove('show'),2600); }
function escapeHtml(s) { return String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function formError(id,message='') { $(id).textContent=message; }

async function init() {
  try {
    state.config=await api('/api/config');
    if (!isAssessmentTab) {
      // The portal always starts at candidate login. Never restore an old result here.
      $('question-count').textContent=state.config.questionCount;
      showView('welcome-view');
      return;
    }
    try {
      const current=await api('/api/exams/current');
      if (current.review) {
        state.result=current; renderResult(); showView('result-view');
      } else {
        state.exam=current; state.index=Math.max(0,current.state.answers.findIndex(a=>a<0));
        renderExam(); showView('exam-view'); startTimer();
      }
    } catch (_) {
      document.querySelector('main').innerHTML = '<section class="assessment-load-error"><h1>Assessment unavailable</h1><p>This assessment session has ended or is no longer available. Return to the portal and start a new assessment.</p></section>';
    }
  } catch(e) {
    if (isAssessmentTab) {
      document.querySelector('main').innerHTML = '<section class="assessment-load-error"><h1>Assessment service unavailable</h1><p>Refresh this tab or return to the portal and try again.</p></section>';
    } else formError('login-error','The examination service is unavailable. Refresh the page or try again later.');
  }
}
$('login-form').addEventListener('submit', e => {
  e.preventDefault(); formError('login-error');
  const name=$('student-name').value.trim(), rollNumber=$('roll-number').value.trim(), subject=$('subject').value;
  if(!name||!rollNumber){formError('login-error','Enter your name and roll number.');return;}
  state.pending={name,rollNumber,subject}; $('instruction-subject').textContent=subject; $('confirm-rules').checked=false; $('begin-exam').disabled=true; showView('instructions-view');
});
$('back-login').addEventListener('click',()=>showView('welcome-view'));
$('confirm-rules').addEventListener('change',e=>$('begin-exam').disabled=!e.target.checked);
$('begin-exam').addEventListener('click',async()=>{
  if(!state.pending||!$('confirm-rules').checked)return;
  // Open synchronously during the user click so browsers do not block the new tab.
  const assessmentTab=window.open('about:blank','_blank');
  if(!assessmentTab){formError('instruction-error','Your browser blocked the assessment tab. Allow pop-ups for this site and try again.');return;}
  assessmentTab.opener=null;
  $('begin-exam').disabled=true; $('begin-exam').textContent='Preparing assessment…'; formError('instruction-error');
  try {
    await api('/api/exams',state.pending);
    assessmentTab.location.replace('/?assessment=active');
    $('begin-exam').textContent='Assessment opened in new tab';
    $('instruction-error').textContent='The examination is running in the new tab. Keep this portal available until you finish.';
  } catch(e){assessmentTab.close();formError('instruction-error',e.message);$('begin-exam').disabled=false;$('begin-exam').innerHTML='Begin examination <span>→</span>';}
});
function currentState(){return state.exam.state;}
function renderExam(){
  const e=state.exam, s=currentState(), q=e.questions[state.index];
  $('exam-subject-title').textContent=e.subject+' examination';
  $('candidate-name').textContent=e.studentName;
  $('candidate-roll').textContent=e.rollNumber;
  if ($('exam-student-line')) $('exam-student-line').textContent=e.studentName+' · '+e.rollNumber;
  $('question-number').textContent='QUESTION '+String(state.index+1).padStart(2,'0'); $('question-text').textContent=q.text;
  $('options-list').innerHTML=q.options.map((option,i)=>`<button type="button" class="option ${s.answers[state.index]===i?'selected':''}" data-option="${i}"><span class="option-letter">${letters[i]}</span><span class="option-text">${escapeHtml(option)}</span></button>`).join('');
  document.querySelectorAll('[data-option]').forEach(btn=>btn.addEventListener('click',()=>chooseAnswer(Number(btn.dataset.option))));
  $('previous-question').disabled=state.index===0; $('next-question').innerHTML=state.index===e.questions.length-1?'Finish review <span>→</span>':'Save &amp; next <span>→</span>';
  $('mark-review').textContent=s.marked[state.index]?'★ Remove review mark':'☆ Mark for review';
  $('navigator-count').textContent=String(state.index+1).padStart(2,'0')+'/'+e.questions.length;
  const answered=s.answers.filter(a=>a>=0).length, review=s.marked.filter(Boolean).length;
  $('stat-answered').textContent=answered; $('stat-review').textContent=review; $('stat-unanswered').textContent=e.questions.length-answered;
  $('answered-label').textContent=answered+' answered'; $('progress-label').textContent='Question '+(state.index+1)+' of '+e.questions.length; $('progress-fill').style.width=(answered/e.questions.length*100)+'%';
  $('question-palette').innerHTML=e.questions.map((_,i)=>{
    const status=i===state.index?'current':s.marked[i]?'review':s.answers[i]>=0?'answered':s.visited[i]?'seen':'unseen';
    const labels={answered:'Answered',current:'Current',seen:'Seen, unanswered',review:'Review',unseen:'Not visited'};
    return `<button class="palette-button ${status}" data-question="${i}" type="button" aria-label="Question ${i+1}: ${labels[status]}" title="${labels[status]}">${i+1}</button>`;
  }).join('');
  document.querySelectorAll('[data-question]').forEach(btn=>btn.addEventListener('click',()=>goTo(Number(btn.dataset.question))));
}
async function chooseAnswer(option){try{state.exam.state=await api('/api/exams/answer',{index:state.index,option});renderExam();}catch(e){toast(e.message);}}
async function goTo(index){if(index<0||index>=state.exam.questions.length)return;try{state.exam.state=await api('/api/exams/visit',{index});state.index=index;renderExam();}catch(e){toast(e.message);}}
$('previous-question').addEventListener('click',()=>goTo(state.index-1));
$('next-question').addEventListener('click',()=>{if(state.index===state.exam.questions.length-1)openSubmitModal();else goTo(state.index+1);});
$('clear-answer').addEventListener('click',async()=>{try{state.exam.state=await api('/api/exams/clear',{index:state.index});renderExam();toast('Response cleared.');}catch(e){toast(e.message);}});
$('mark-review').addEventListener('click',async()=>{try{const marked=!state.exam.state.marked[state.index];state.exam.state=await api('/api/exams/mark',{index:state.index,marked});renderExam();}catch(e){toast(e.message);}});
function startTimer(){clearInterval(state.timerId);tick();state.timerId=setInterval(tick,1000);}
function tick(){if(!state.exam)return;const remaining=Math.max(0,Math.ceil((state.exam.endsAt-Date.now())/1000));$('timer').textContent=String(Math.floor(remaining/60)).padStart(2,'0')+':'+String(remaining%60).padStart(2,'0');$('timer-card').classList.toggle('warning',remaining<=60);$('timer-status').textContent=remaining<=60?'Less than one minute remaining':'Your time is running';if(remaining<=0){clearInterval(state.timerId);submitExam(true);}}
function openSubmitModal(){const answered=state.exam.state.answers.filter(a=>a>=0).length;$('submit-summary').textContent=`You have answered ${answered} of ${state.exam.questions.length} questions. ${state.exam.questions.length-answered} will remain unanswered.`;$('submit-modal').classList.add('open');$('submit-modal').setAttribute('aria-hidden','false');}
function closeModals(){document.querySelectorAll('.modal-backdrop').forEach(m=>{m.classList.remove('open');m.setAttribute('aria-hidden','true');});}
document.querySelectorAll('[data-close-modal]').forEach(b=>b.addEventListener('click',closeModals));
$('submit-exam').addEventListener('click',openSubmitModal);$('confirm-submit').addEventListener('click',()=>submitExam(false));
async function submitExam(auto){if(state.busy)return;state.busy=true;closeModals();$('confirm-submit').disabled=true;try{state.result=await api('/api/exams/submit',{});clearInterval(state.timerId);renderResult();showView('result-view');}catch(e){toast(e.message);state.busy=false;$('confirm-submit').disabled=false;if(auto)toast('Time has expired. Please submit again.');}}
function renderResult(){const r=state.result;$('result-title').textContent=r.passed?'Assessment completed.':'Assessment submitted.';$('result-subtitle').textContent=r.studentName+' · '+r.rollNumber+' · '+r.subject;$('score-value').textContent=r.correctCount;$('metric-percent').textContent=r.percentage+'%';$('metric-correct').textContent=r.correctCount;$('metric-wrong').textContent=r.wrong;$('metric-skipped').textContent=r.skipped;$('grade-value').textContent=r.grade;$('pass-status').textContent=r.passed?'Passed':'Not passed';$('pass-status').classList.toggle('pass',r.passed);$('time-used').textContent='Time used · '+formatTime(r.secondsUsed);$('score-fill').style.width=r.percentage+'%';$('score-message').textContent=r.passed?'You met the passing threshold. Review your answers below.':'Keep practising. Review the explanations and try again.';if(r.timedOut)toast('Time expired. Your assessment was submitted automatically.');
  $('review-list').innerHTML=r.review.map(item=>{const selected=item.selected<0?'Not answered':item.options[item.selected];const correct=item.options[item.correct];return `<article class="review-item"><div class="review-item-top"><strong>QUESTION ${String(item.number).padStart(2,'0')}</strong><span class="review-status ${item.status}">${item.status}</span></div><p class="review-question">${escapeHtml(item.question)}</p><div class="review-answers"><div class="review-answer ${item.status==='Incorrect'?'your-wrong':''}"><strong>Your response</strong>${escapeHtml(selected)}</div><div class="review-answer correct-answer"><strong>Correct answer</strong>${escapeHtml(correct)}</div></div></article>`;}).join('');}
function formatTime(s){return String(Math.floor(s/60)).padStart(2,'0')+':'+String(s%60).padStart(2,'0');}
async function resetHome(){
  state.exam=null;state.result=null;state.pending=null;state.busy=false;clearInterval(state.timerId);
  try { await api('/api/session/reset',{}); } catch (_) { /* The portal remains usable; the next attempt replaces the session attempt. */ }
  $('login-form').reset();formError('login-error');showView('welcome-view');
}
$('new-exam').addEventListener('click',resetHome);$('home-button').addEventListener('click',resetHome);
$('admin-open').addEventListener('click',()=>{$('admin-modal').classList.add('open');$('admin-modal').setAttribute('aria-hidden','false');$('admin-pin').focus();});
$('admin-form').addEventListener('submit',async e=>{e.preventDefault();formError('admin-error');try{await api('/api/admin/login',{pin:$('admin-pin').value});const data=await api('/api/admin/results');closeModals();renderAdminResults(data.results);}catch(err){formError('admin-error',err.message);}});
function renderAdminResults(rows){const html=`<section class="view active" id="admin-results-view"><button class="back-link" id="admin-back">← Back to portal</button><div class="section-heading"><div class="eyebrow">ADMINISTRATION</div><h1>Assessment records</h1><p>${rows.length} submitted assessment${rows.length===1?'':'s'} recorded on this server.</p></div><div class="admin-table-wrap"><table class="admin-table"><thead><tr><th>Student</th><th>Roll number</th><th>Subject</th><th>Score</th><th>Grade</th><th>Status</th><th>Submitted</th></tr></thead><tbody>${rows.map(r=>`<tr><td>${escapeHtml(r.studentName)}</td><td>${escapeHtml(r.rollNumber)}</td><td>${escapeHtml(r.subject)}</td><td>${r.correct}/${r.total}</td><td>${escapeHtml(r.grade)}</td><td>${r.passed?'Passed':'Not passed'}</td><td>${escapeHtml(new Date(r.submittedAt).toLocaleString())}</td></tr>`).join('')||'<tr><td colspan="7">No results recorded yet.</td></tr>'}</tbody></table></div></section>`;document.querySelector('main').insertAdjacentHTML('beforeend',html);document.querySelectorAll('.view').forEach(v=>v.classList.toggle('active',v.id==='admin-results-view'));$('admin-back').addEventListener('click',()=>{ $('admin-results-view').remove();showView('welcome-view');});}
init();
