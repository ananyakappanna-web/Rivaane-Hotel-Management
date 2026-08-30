const API = '/api';
const byId = id => document.getElementById(id);
const adminToken = () => localStorage.getItem('rivaaneAdminToken');
const esc = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[character]));
const money = value => new Intl.NumberFormat('en-IN', {style:'currency', currency:'INR', maximumFractionDigits:0}).format(value);

// Sticky Navbar with scroll effect
window.addEventListener('scroll', () => {
    const nav = document.querySelector('.nav');
    if (nav) {
        if (window.scrollY > 50) {
            nav.classList.add('scrolled');
        } else {
            nav.classList.remove('scrolled');
        }
    }
});

// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Add fade-in animation on scroll
const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('fade-in');
            observer.unobserve(entry.target);
        }
    });
}, observerOptions);

// Observe sections for animation
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('section').forEach(section => {
        observer.observe(section);
    });
});

async function apiFetch(path, options = {}) {
    options.headers = {'Content-Type':'application/json', ...(options.headers || {})};
    if (adminToken()) options.headers['X-Admin-Token'] = adminToken();
    const response = await fetch(API + path, options);
    const data = await response.json().catch(() => ({}));
    if (response.status === 401 && document.querySelector('.dashboard-page')) {
        localStorage.removeItem('rivaaneAdminToken');
        window.location.href = 'login.html';
    }
    if (!response.ok) throw new Error(data.message || 'The request could not be completed.');
    return data;
}

function toast(message, isError = false) {
    const element = byId('toast');
    if (!element) return;
    element.textContent = message;
    element.classList.add('show');
    element.style.background = isError ? '#a84f4f' : '';
    window.setTimeout(() => element.classList.remove('show'), 3200);
}

function roomImage(key) {
    return ({royal:'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1000&q=85', deluxe:'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1000&q=85', executive:'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1000&q=85', garden:'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1000&q=85', presidential:'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1000&q=85', family:'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1000&q=85'})[key] || 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1000&q=85';
}

function roomMarkup(room, dates = []) {
    return `<article class="room-card"><div class="room-image" style="background-image:url('${roomImage(room.image)}')"></div><div class="room-content"><p class="eyebrow">${esc(room.type)}</p><h3>${esc(room.name)}</h3><p class="muted">${esc(room.description)}</p><div class="room-footer"><b>${money(room.price)} <small>/ night</small></b><button class="button" onclick='openBooking(${JSON.stringify(room)},${JSON.stringify(dates)})'>Reserve</button></div></div></article>`;
}

async function loadRooms() {
    const grid = byId('roomGrid');
    if (!grid) return;
    try {
        const search = encodeURIComponent(byId('roomSearch')?.value || '');
        const type = encodeURIComponent(byId('roomType')?.value || 'all');
        const rooms = await apiFetch(`/rooms?search=${search}&type=${type}`);
        grid.innerHTML = rooms.length ? rooms.map(room => roomMarkup(room)).join('') : '<p class="muted">No rooms found.</p>';
    } catch (error) { grid.innerHTML = `<p class="notice">${esc(error.message)}</p>`; }
}

async function checkAvailability() {
    const checkIn = byId('checkIn')?.value, checkOut = byId('checkOut')?.value, message = byId('availabilityMessage');
    if (!checkIn || !checkOut) { message.textContent = 'Please select both dates.'; return; }
    try { const rooms = await apiFetch(`/rooms/available?checkIn=${checkIn}&checkOut=${checkOut}`); message.textContent = `${rooms.length} room(s) available for your stay.`; byId('roomGrid').innerHTML = rooms.map(room => roomMarkup(room, [checkIn, checkOut])).join(''); }
    catch (error) { message.textContent = error.message; }
}

function openBooking(room, dates = []) { byId('bookingRoomId').value = room.id; byId('bookingRoomName').textContent = room.name; byId('bookingCheckIn').value = dates[0] || ''; byId('bookingCheckOut').value = dates[1] || ''; byId('bookingResult').innerHTML = ''; byId('bookingModal').classList.add('show'); }
function closeBooking() { byId('bookingModal')?.classList.remove('show'); }

async function submitBooking(event) {
    event.preventDefault();
    const data = {roomId:Number(byId('bookingRoomId').value), name:byId('guestName').value, email:byId('guestEmail').value, phone:byId('guestPhone').value, checkIn:byId('bookingCheckIn').value, checkOut:byId('bookingCheckOut').value, paymentMethod:document.querySelector('[name=payment]:checked').value};
    try { const result = await apiFetch('/bookings', {method:'POST', body:JSON.stringify(data)}); byId('bookingResult').innerHTML = `<p class="eyebrow">RESERVATION CONFIRMED</p><h3>${esc(result.reservationId)}</h3><p>Thank you, ${esc(data.name)}. Total: <b>${money(result.total)}</b></p>`; byId('bookingForm').reset(); toast('Reservation confirmed.'); }
    catch (error) { byId('bookingResult').innerHTML = `<p class="notice">${esc(error.message)}</p>`; }
}

async function submitLogin(event) { event.preventDefault(); try { const result = await apiFetch('/auth/login', {method:'POST', body:JSON.stringify({email:byId('loginEmail').value, password:byId('loginPassword').value})}); localStorage.setItem('rivaaneAdminToken', result.token); window.location.href = 'dashboard.html'; } catch (error) { byId('loginMessage').textContent = error.message; } }
async function logout() { await apiFetch('/auth/logout', {method:'POST'}).catch(() => {}); localStorage.removeItem('rivaaneAdminToken'); window.location.href = 'login.html'; }
async function checkAdminAuth() { if (!adminToken()) { window.location.href = 'login.html'; return false; } try { await apiFetch('/dashboard'); return true; } catch { return false; } }

function showPanel(id, button) { document.querySelectorAll('.panel').forEach(panel => panel.classList.remove('active')); document.querySelectorAll('.side').forEach(link => link.classList.remove('active')); byId(id)?.classList.add('active'); button?.classList.add('active'); const loaders = {overview:loadDashboard, roomsAdmin:loadAdminRooms, reservations:loadReservations, guests:loadGuests, staff:loadStaff, billing:loadInvoices, housekeeping:loadHousekeeping, concepts:loadConcepts}; loaders[id]?.(); }
function setText(id, value) { if (byId(id)) byId(id).textContent = value; }

async function loadDashboard() { try { const data = await apiFetch('/dashboard'); [['statRooms',data.totalRooms],['statAvailable',data.available],['statReserved',data.reserved],['statOccupied',data.occupied],['statCleaning',data.cleaning],['statReservations',data.activeBookings],['statGuests',data.totalGuests],['statStaff',data.totalStaff],['statRevenue',money(data.revenue)]].forEach(pair => setText(...pair)); } catch (error) { toast(error.message, true); } }

async function loadAdminRooms() {
    const table = byId('roomsTable'); if (!table) return;
    try { const rooms = await apiFetch(`/admin/rooms?search=${encodeURIComponent(byId('adminRoomSearch')?.value || '')}`); table.innerHTML = rooms.length ? rooms.map(room => `<tr><td><strong>${esc(room.name)}</strong></td><td>${esc(room.type)}</td><td>${money(room.price)}</td><td>${room.capacity}</td><td>${esc(room.status)}</td><td><button class="outline" onclick='editRoom(${JSON.stringify(room)})'>Edit</button> <button class="danger-button" onclick="deleteRoom(${room.id})">Delete</button></td></tr>`).join('') : '<tr><td colspan="6">No rooms found.</td></tr>'; }
    catch (error) { table.innerHTML = `<tr><td colspan="6" class="notice">${esc(error.message)}</td></tr>`; }
}

function openRoomForm(room = null) { byId('roomForm').reset(); byId('editRoomId').value = room?.id || ''; byId('roomModalTitle').textContent = room ? 'Edit room' : 'Add room'; byId('roomFormMessage').textContent = ''; if (room) editRoom(room, false); byId('roomModal').classList.add('show'); }
function editRoom(room, open = true) { byId('editRoomId').value = room.id; byId('roomName').value = room.name || ''; byId('roomTypeAdmin').value = room.type || 'Standard'; byId('roomPrice').value = room.price || ''; byId('roomCapacity').value = room.capacity || ''; byId('roomDescription').value = room.description || ''; byId('roomImage').value = room.image || ''; byId('roomModalTitle').textContent = 'Edit room'; if (open) byId('roomModal').classList.add('show'); }
function closeRoomForm() { byId('roomModal')?.classList.remove('show'); }

async function saveRoom(event) {
    event.preventDefault();
    const id = byId('editRoomId').value;
    const data = {name:byId('roomName').value, type:byId('roomTypeAdmin').value, price:Number(byId('roomPrice').value), capacity:Number(byId('roomCapacity').value), description:byId('roomDescription').value, image:byId('roomImage').value};
    try { await apiFetch(id ? `/admin/rooms/${id}` : '/admin/rooms', {method:id ? 'PUT' : 'POST', body:JSON.stringify(data)}); closeRoomForm(); toast(id ? 'Room updated successfully.' : 'Room added successfully.'); await Promise.all([loadAdminRooms(), loadDashboard()]); }
    catch (error) { byId('roomFormMessage').textContent = error.message; toast(error.message, true); }
}

async function deleteRoom(id) { if (!window.confirm('Are you sure you want to delete this room?')) return; try { await apiFetch(`/admin/rooms/${id}`, {method:'DELETE'}); toast('Room deleted successfully.'); await Promise.all([loadAdminRooms(), loadDashboard()]); } catch (error) { toast(error.message, true); } }

async function loadReservations() {
    const table = byId('reservationsTable'); if (!table) return;
    try { const [reservations, rooms] = await Promise.all([apiFetch('/admin/reservations'), apiFetch('/admin/rooms')]); table.innerHTML = reservations.length ? reservations.map(reservation => { const room = rooms.find(item => item.id === reservation.roomId); let actions = ''; if (reservation.status === 'CONFIRMED') actions += `<button class="outline" onclick="checkIn('${reservation.reservationId}')">Check-in</button>`; if (reservation.status === 'CHECKED_IN') actions += `<button class="outline" onclick="checkOut('${reservation.reservationId}')">Check-out</button>`; if (!['CANCELLED','CHECKED_OUT'].includes(reservation.status)) actions += ` <button class="danger-button" onclick="cancelAdminBooking('${reservation.reservationId}')">Cancel</button>`; return `<tr><td>${esc(reservation.reservationId)}</td><td>${esc(reservation.guestName)}</td><td>${esc(room?.name || 'Unknown')}</td><td>${reservation.checkIn}</td><td>${reservation.checkOut}</td><td>${money(reservation.total)}</td><td>${esc(reservation.status)}</td><td>${actions || '—'}</td></tr>`; }).join('') : '<tr><td colspan="8">No reservations found.</td></tr>'; } catch (error) { table.innerHTML = `<tr><td colspan="8" class="notice">${esc(error.message)}</td></tr>`; }
}
async function checkIn(id) { try { await apiFetch(`/admin/reservations/${id}/checkin`, {method:'POST'}); toast('Guest checked in.'); await Promise.all([loadReservations(), loadAdminRooms(), loadDashboard()]); } catch (error) { toast(error.message, true); } }
async function checkOut(id) { try { await apiFetch(`/admin/reservations/${id}/checkout`, {method:'POST'}); toast('Guest checked out. Room sent to cleaning.'); await Promise.all([loadReservations(), loadAdminRooms(), loadHousekeeping(), loadDashboard()]); } catch (error) { toast(error.message, true); } }
async function cancelAdminBooking(id) { if (!window.confirm('Cancel this reservation?')) return; try { await apiFetch(`/bookings/${id}/cancel`, {method:'POST'}); toast('Reservation cancelled.'); await Promise.all([loadReservations(), loadAdminRooms(), loadDashboard()]); } catch (error) { toast(error.message, true); } }

async function loadGuests() { const table = byId('guestsTable'); if (!table) return; try { const guests = await apiFetch(`/admin/guests?search=${encodeURIComponent(byId('guestSearch')?.value || '')}`); table.innerHTML = guests.length ? guests.map(guest => `<tr><td><strong>${esc(guest.name)}</strong></td><td>${esc(guest.email)}</td><td>${esc(guest.phone)}</td></tr>`).join('') : '<tr><td colspan="3">No guests found.</td></tr>'; } catch (error) { table.innerHTML = `<tr><td colspan="3" class="notice">${esc(error.message)}</td></tr>`; } }

async function loadStaff() {
    const table = byId('staffTable'); if (!table) return;
    try { const staff = await apiFetch(`/admin/staff?search=${encodeURIComponent(byId('staffSearch')?.value || '')}`); table.innerHTML = staff.length ? staff.map(s => `<tr><td><strong>${esc(s.name)}</strong></td><td>${esc(s.email)}</td><td>${esc(s.phone)}</td><td>${esc(s.position)}</td><td>${esc(s.department)}</td><td>${money(s.salary)}</td><td>${esc(s.status)}</td><td><button class="outline" onclick='editStaff(${JSON.stringify(s)})'>Edit</button> <button class="danger-button" onclick="deleteStaff(${s.id})">Delete</button></td></tr>`).join('') : '<tr><td colspan="8">No staff found.</td></tr>'; }
    catch (error) { table.innerHTML = `<tr><td colspan="8" class="notice">${esc(error.message)}</td></tr>`; }
}

function openStaffForm(staffMember = null) { byId('staffForm').reset(); byId('editStaffId').value = staffMember?.id || ''; byId('staffModalTitle').textContent = staffMember ? 'Edit staff' : 'Add staff'; byId('staffFormMessage').textContent = ''; if (staffMember) editStaff(staffMember, false); byId('staffModal').classList.add('show'); }
function editStaff(staffMember, open = true) { byId('editStaffId').value = staffMember.id; byId('staffName').value = staffMember.name || ''; byId('staffEmail').value = staffMember.email || ''; byId('staffPhone').value = staffMember.phone || ''; byId('staffPosition').value = staffMember.position || ''; byId('staffDepartment').value = staffMember.department || ''; byId('staffSalary').value = staffMember.salary || ''; byId('staffStatus').value = staffMember.status || 'ACTIVE'; byId('staffModalTitle').textContent = 'Edit staff'; if (open) byId('staffModal').classList.add('show'); }
function closeStaffForm() { byId('staffModal')?.classList.remove('show'); }

async function saveStaff(event) {
    event.preventDefault();
    const id = byId('editStaffId').value;
    const data = {name:byId('staffName').value, email:byId('staffEmail').value, phone:byId('staffPhone').value, position:byId('staffPosition').value, department:byId('staffDepartment').value, salary:Number(byId('staffSalary').value), status:byId('staffStatus').value};
    try { await apiFetch(id ? `/admin/staff/${id}` : '/admin/staff', {method:id ? 'PUT' : 'POST', body:JSON.stringify(data)}); closeStaffForm(); toast(id ? 'Staff updated successfully.' : 'Staff added successfully.'); await loadStaff(); }
    catch (error) { byId('staffFormMessage').textContent = error.message; toast(error.message, true); }
}

async function deleteStaff(id) { if (!window.confirm('Are you sure you want to delete this staff member?')) return; try { await apiFetch(`/admin/staff/${id}`, {method:'DELETE'}); toast('Staff member deleted successfully.'); await loadStaff(); } catch (error) { toast(error.message, true); } }

async function loadInvoices() {
    const table = byId('invoicesTable'); if (!table) return;
    try { const invoices = await apiFetch('/admin/invoices'); table.innerHTML = invoices.length ? invoices.map(inv => `<tr><td>${esc(inv.invoiceId)}</td><td>${esc(inv.guestName)}</td><td>${esc(inv.reservationId)}</td><td>${money(inv.total)}</td><td>${money(inv.tax)}</td><td>${money(inv.discount)}</td><td>${money(inv.grandTotal)}</td><td>${esc(inv.status)}</td><td><button class="outline" onclick="printInvoice('${inv.invoiceId}')">Print</button> <button class="outline" onclick="updateInvoiceStatus('${inv.invoiceId}', 'PAID')">Mark Paid</button></td></tr>`).join('') : '<tr><td colspan="9">No invoices found.</td></tr>'; }
    catch (error) { table.innerHTML = `<tr><td colspan="9" class="notice">${esc(error.message)}</td></tr>`; }
}

async function openInvoiceForm() { byId('invoiceForm').reset(); byId('invoiceFormMessage').textContent = ''; try { const reservations = await apiFetch('/admin/reservations'); const select = byId('invoiceReservation'); select.innerHTML = '<option value="">Select reservation</option>' + reservations.filter(r => r.status === 'CHECKED_OUT').map(r => `<option value="${r.reservationId}">${r.reservationId} - ${r.guestName} (${money(r.total)})</option>`).join(''); byId('invoiceModal').classList.add('show'); } catch (error) { byId('invoiceFormMessage').textContent = error.message; } }
function closeInvoiceForm() { byId('invoiceModal')?.classList.remove('show'); }

async function saveInvoice(event) {
    event.preventDefault();
    const reservationId = byId('invoiceReservation').value;
    const tax = Number(byId('invoiceTax').value);
    const discount = Number(byId('invoiceDiscount').value);
    try { const result = await apiFetch('/admin/invoices', {method:'POST', body:JSON.stringify({reservationId, tax, discount})}); closeInvoiceForm(); toast('Invoice generated successfully.'); await loadInvoices(); }
    catch (error) { byId('invoiceFormMessage').textContent = error.message; toast(error.message, true); }
}

async function updateInvoiceStatus(invoiceId, status) { try { await apiFetch(`/admin/invoices/${invoiceId}/status`, {method:'PUT', body:JSON.stringify({status})}); toast('Invoice status updated.'); await loadInvoices(); } catch (error) { toast(error.message, true); } }

function printInvoice(invoiceId) { const printWindow = window.open('', '_blank'); printWindow.document.write(`<html><head><title>Invoice ${invoiceId}</title><style>body{font-family:Arial,sans-serif;padding:20px}h1{color:#10232c}.invoice-details{margin:20px 0}table{width:100%;border-collapse:collapse;margin:20px 0}th,td{border:1px solid #ddd;padding:10px;text-align:left}th{background:#f5f0e7}.total{font-weight:bold;font-size:1.2em;margin-top:20px}</style></head><body><h1>RIVAANE HOTEL</h1><p>Where Heritage Meets Luxury</p><div class="invoice-details"><h2>Invoice: ${invoiceId}</h2><p>Thank you for your stay!</p></div><table><tr><th>Description</th><th>Amount</th></tr><tr><td>Room Charges</td><td>Calculating...</td></tr><tr><td>Tax</td><td>Calculating...</td></tr><tr><td>Discount</td><td>Calculating...</td></tr><tr class="total"><td>Grand Total</td><td>Calculating...</td></tr></table><p><small>Generated by RIVAANE Hotel Management System</small></p></body></html>`); printWindow.document.close(); printWindow.print(); }
async function loadHousekeeping() {
    const grid = byId('housekeepingGrid'); if (!grid) return;
    try {
        const [tasks, analytics] = await Promise.all([apiFetch('/admin/housekeeping'), apiFetch('/admin/housekeeping/analytics')]);
        byId('housekeepingAnalytics').innerHTML = `Cleaned <b>${analytics.roomsCleaned}</b> In progress <b>${analytics.inProgress}</b> Outstanding <b>${analytics.outstanding}</b> Average minutes <b>${Math.round(analytics.averageCleaningMinutes)}</b>`;
        grid.innerHTML = tasks.map(task => `<article class="card"><p class="eyebrow">ROOM ${task.roomId}</p><h3>${esc(task.status)}</h3><label>Status<select id="status-${task.roomId}"><option ${task.status === 'CLEAN' ? 'selected' : ''}>CLEAN</option><option ${task.status === 'DIRTY' ? 'selected' : ''}>DIRTY</option><option ${task.status === 'IN_PROGRESS' ? 'selected' : ''}>IN_PROGRESS</option><option ${task.status === 'OUT_OF_SERVICE' ? 'selected' : ''}>OUT_OF_SERVICE</option></select></label><input id="staff-${task.roomId}" value="${esc(task.assignedStaff)}" placeholder="Assigned staff"><input id="priority-${task.roomId}" value="${esc(task.priority)}" placeholder="Priority"><input id="date-${task.roomId}" type="date" value="${esc(task.scheduledDate || '')}"><textarea id="notes-${task.roomId}" placeholder="Notes">${esc(task.notes)}</textarea><button class="button" onclick="saveHousekeepingTask(${task.roomId})">Save task</button></article>`).join('');
    } catch (error) { grid.innerHTML = `<p class="notice">${esc(error.message)}</p>`; }
}
async function saveHousekeepingTask(roomId) { try { await apiFetch(`/admin/housekeeping/${roomId}`, {method:'PUT', body:JSON.stringify({status:byId(`status-${roomId}`).value, assignedStaff:byId(`staff-${roomId}`).value, priority:byId(`priority-${roomId}`).value, scheduledDate:byId(`date-${roomId}`).value, notes:byId(`notes-${roomId}`).value})}); toast('Housekeeping task updated.'); await Promise.all([loadHousekeeping(), loadAdminRooms(), loadDashboard()]); } catch (error) { toast(error.message, true); } }
async function markClean(id) { try { await apiFetch(`/admin/rooms/${id}/clean`, {method:'POST'}); toast('Room is now available. Reception notified.'); await Promise.all([loadHousekeeping(), loadAdminRooms(), loadDashboard()]); } catch (error) { toast(error.message, true); } }
async function loadConcepts() { const grid = byId('conceptGrid'); if (!grid) return; try { const concepts = await apiFetch('/concepts'); grid.innerHTML = concepts.map(concept => `<article class="card"><p class="eyebrow">${esc(concept.name)}</p><p class="muted">${esc(concept.description)}</p></article>`).join(''); } catch (error) { grid.innerHTML = `<p class="notice">${esc(error.message)}</p>`; } }

document.addEventListener('DOMContentLoaded', async () => { 
    // Initialize animations
    document.querySelectorAll('section').forEach(section => {
        observer.observe(section);
    });
    
    // Load initial content
    if (byId('roomGrid')) loadRooms(); 
    byId('bookingForm')?.addEventListener('submit', submitBooking); 
    byId('loginForm')?.addEventListener('submit', submitLogin); 
    byId('roomForm')?.addEventListener('submit', saveRoom); 
    byId('staffForm')?.addEventListener('submit', saveStaff); 
    byId('invoiceForm')?.addEventListener('submit', saveInvoice); 
    if (document.querySelector('.dashboard-page') && await checkAdminAuth()) loadDashboard(); 
    document.querySelectorAll('.modal').forEach(modal => modal.addEventListener('click', event => { if (event.target === modal) modal.classList.remove('show'); })); 
});
