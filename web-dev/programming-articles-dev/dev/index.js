const $ = s => document.querySelector(s);
const $id = s => document.getElementById(s);
const loading_indicator = $id("loading_indicator");
const container = document.getElementById("container");

const { el, mount, list } = require("redom");
const clone = require("lodash/clone");
const isEqual = require("lodash/isEqual");
const BASE_PATH = (() => {
  const s = window.location.pathname;
  if(s.lastIndexOf('/') !== 0)
    return s.substring(0, s.lastIndexOf('/'));
  return s;
})();
const BASE_DATA_PATH = window.BASE_DATA_PATH || BASE_PATH.concat("/data/");

const utils = {
  setAttr(target, field, value) {
    if (target[field] !== value) {
      target[field] = value;
    }
  },
  visible(el) {
    el.classList.add("visible");
  },
  invisible(el) {
    el.classList.remove("visible");
  },
  hide(el) {
    if (el) el.el.classList.add("hidden");
  },
  unhide(el) {
    el.el.classList.remove("hidden");
  },
  disable(el, value = true) {
    el.disabled = value;
  },
  remove(array, item) {
    const n = array.indexOf(item);
    if (n >= 0) array.splice(n, 1);
  },
  setLink(el, url) {
    el.href = url || "";
    el.innerText = url || "";
  },
  setText(el, text) {
    el.innerText = text || "";
  },
  nearestParent(child, tagName) {
    while (true) {
      if (!child || child.tagName === tagName) {
        return child;
      }
      child = child.parentElement;
    }
  }
};

Object.freeze(utils);

const http = {
  async GET(url, contentType = "application/json;charset=UTF-8") {
    if (!url) {
      throw new Error(`bad request: url: ${url}, body: ${body}`);
    }
    utils.visible(loading_indicator);
    try {
      const result = await fetch(url, {
        method: "GET",
        headers: { "Content-Type": contentType }
      });
      utils.invisible(loading_indicator);
      return result;
    } catch (err) {
      utils.invisible(loading_indicator);
      alert(err.toString());
    }
  },
  async POST(url, body, contentType = "application/json;charset=UTF-8") {
    if (!url || !body) {
      throw new Error(`bad request: url: ${url}, body: ${body}`);
    }
    utils.visible(loading_indicator);
    try {
      const result = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": contentType },
        body: typeof body === "string" ? body : JSON.stringify(body)
      });
      utils.invisible(loading_indicator);
      return result;
    } catch (err) {
      utils.invisible(loading_indicator);
      alert(err.toString());
    }
  }
};

const stateManger = {
  statusMap: new Map(),
  touched: new Set(),
async metas() {
  return stateManger.metas = (await (await http.GET(BASE_DATA_PATH.concat("metas"))).json());
},
  async loadPage(pageNum, pageSize, status, startingId) {
    const params = {
      page: pageNum || stateManger.pageNum || 0,
      pageSize: pageSize || stateManger.pageSize,
      status: status || stateManger.status,
      startingId: startingId || stateManger.startingId
    };
    const query = Object.keys(params)
      .filter(s => params[s] || params[s] === 0)
      .map(s => s + "=" + params[s])
      .join("&");
    if (!query) {
      throw new Error("no query make");
    }
    const res = await http.GET(BASE_DATA_PATH.concat("page?").concat(query));
    stateManger.page = Object.freeze(await res.json());
    stateManger.pageNum = params.page;
    return stateManger.page;
  },
  async loadItemByIndex(index) {
    if (!this.page || index > this.page.data.length || index < 0) {
      throw new Error("index not found: " + index);
    }
    if(index == this.page.data.length) {
      await stateManger.loadPage(0, undefined, undefined, this.page.data[index - 1].id);
      if(stateManger.page.data.length === 0) {
        return null;
      }
      index = 0;
    }
    const item = this.page.data[index];
    if(!stateManger.touched.has(item.id)) {
      stateManger.touched.add(item.id);
      item.tags = item.tags_parsed;
      stateManger.item = item;
    } else {
      stateManger.item = await stateManger.loadItem(item.id);
    }
    stateManger.dataIndex = index;
    return stateManger.item;
  },
  async loadItem(itemId) {
    const res = await http.GET(BASE_DATA_PATH.concat("item?itemId=" + itemId));
    const item = await res.json();
    if(stateManger.page) {
      stateManger.dataIndex = stateManger.page.data.findIndex(item => item.id === itemId); 
      if(stateManger.dataIndex <  0)
      stateManger.dataIndex = 0;
    }
    item.tags = item.tags_parsed;
    return stateManger.item = item;
  },
  async tags() {
    const res = await http.GET(BASE_DATA_PATH.concat("tags"), "text/plain");
    return stateManger.allTags = Object.freeze((await res.text()).split(/\r?\n/));
  }
};

// list view

class DataItemLi {
  constructor() {
    this.img = el("img.item-icon");
    this.title = el("span.item-title");
    this.el = el("li", [this.img, this.title]);
  }
  update(data) {
    this.el.id = "list-item-" + data.id;
    this.el.dataset["itemid"] = String(data.id);
    this.title.textContent = data.title;
    utils.setAttr(
      this.img,
      "src",
      data.favicon || BASE_PATH.concat("-assets/layout.png")
    );
  }
}

class ItemList {
  constructor() {
    this.prev_btn = el("button.icon.prev");
    this.next_btn = el("button.icon.next");
    this.page_info = el("span.page_info");
    this.dataItemList = list("ul.center", DataItemLi);

    const loadList = pageNum => stateManger.loadPage(pageNum).then(page => this.update(page.data));
    this.next_btn.onclick = () => loadList(stateManger.pageNum + 1);
    this.prev_btn.onclick = () => loadList(stateManger.pageNum - 1);

    this.el = el("section#items", [
      el(".top", [this.prev_btn, this.page_info, this.next_btn]),
      this.dataItemList
    ]);
    this.dataItemList.el.onclick = e => {
      const target = utils.nearestParent(e.target, "LI");
      if (target && target.dataset.itemid) {
        history.pushState({},target.innerText,BASE_PATH.concat("?item=" + target.dataset.itemid));
        // see: https://felix-kling.de/blog/2011/how-to-detect-history-pushstate.html
        if (typeof history.onpushstate == "function") {  
          history.onpushstate();  
        } 
      }
    };
  }
  update(data) {
    const filter = item => {
      const s = stateManger.statusMap.get(item.id);
      return !s || stateManger.page.status === s; 
    };
    if(data.some(item => !filter(item))) {
      data = data.filter(filter);
    }

    this.dataItemList.update(data);
    utils.disable(this.prev_btn, stateManger.pageNum === 0);
    this.updateCount();
  }
  updateCount() {
    const list = this.dataItemList.el.children;
    let text = `page: ${stateManger.page.page}, count: ${list.length}`;
    if (list.length > 1) {
      text += `, range: ${list[0].dataset.itemid} - ${
        list[list.length - 1].dataset.itemid
      }`;
    }
    this.page_info.innerText = text;
  }
}

class CloseableTag {
  constructor() {
    this.title = el("span");
    this.remove = el("button.remove_tag");
    this.remove.textContent = "x";
    this.el = el("li.tag", [this.title, this.remove]);
  }
  update(item) {
    this.title.textContent = item;
    this.remove.dataset.tag = item;
  }
}

class AddedTags {
  constructor() {
    this.el = list("ul.added_tags", CloseableTag);
    this.el.el.onclick = e => {
      if (e.target.classList.contains("remove_tag")) {
        const s = e.target.dataset.tag;
        utils.remove(this.tags, s);
        this.el.update(this.tags);
        this.onRemove(s);
      }
    };
  }
  add(tag) {
    this.tags.push(tag);
    this.el.update(this.tags);
  }
  set(tags) {
    this.tags = tags || [];
    this.el.update(this.tags);
  }
}

class TagsAdder {
  constructor() {
    this.title = el("h1.title");
    this.searchBox = el("input.searchbox", { type: "text" });
    this.tagsList = list("ul.tags_list", Tag);
    this.addedTags = new AddedTags();

    const ok = el("button.ok_btn", "OK");
    ok.onclick = () => this.onclose(this.addedTags.tags);

    this.el = el("#tags_adder", [
      this.title,
      this.searchBox,
      this.tagsList,
      el("span.added_tags_wrap", [this.addedTags, ok])
    ]);

    this.searchBox.onkeyup = e => this.onsearch(e);

    this.tagsList.el.onclick = e => {
      if (e.target.tagName === "LI") {
        this.addTag(e.target.innerText);
      }
    };
  }

  onsearch(e) {
    if (e.keyCode === 13) {
      if (this.searchBox.value && this.searchBox.value.trim())
        this.addTag(this.searchBox.value.trim());
    } else {
      const text = this.searchBox.value.toLowerCase();
      this.tagsList.update(
        !text
          ? this.loadedTags
          : this.loadedTags.filter(s => s.lowercased.includes(text))
      );
    }
  }

  addTag(tag) {
    this.tagsList.update(this.loadedTags);
    this.addedTags.add(tag);
    this.searchBox.value = "";
  }

  update(title, addedTagsItems) {
    this.title.textContent = title;
    this.addedTagsItems = addedTagsItems;
    if (!stateManger.allTags) {
      stateManger.tags().then(tags => {
        this.loadedTags = Array.from(tags, s => ({
          name: s,
          lowercased: s.toLowerCase()
        }));
        this.tagsList.update(this.loadedTags);
      });
    }
    this.addedTags.set(this.addedTagsItems);
  }
}

class ChangeDetector {
  constructor(target, field) {
    this.target = target;
    this.field = field;
    this.old_state = clone(this.target[field]);
  }
  isChanged() {
    return !isEqual(this.old_state, this.getUpdated());
  }
  set(value) {
    this.target[this.field] = value;
  }
  getUpdated() {
    return this.target[this.field];
  }
  toString() {
    return `ChangeDetector(${this.field})[${this.old_state} -> ${
      this.target[this.field]
    }]`;
  }
}

class ContentPane {
  constructor() {
    this.favicon = el("img.icon");
    this.id_info = el("span.id_info");
    this.title = el("h1.title");
    const trs = [];
    this.source = this.tr(trs, "Source", el("a", { target: "_blank" }));
    this.redirect = this.tr(trs, "Redirect", el("a", { target: "_blank" }));
    this.addedOn = this.tr(trs, "Added On");
    this.status = this.tr(trs, "Status", el( "select.status", stateManger.metas.allStatus.map(s => el("option", { value: s, textContent: s })) ) );
    this.tags = this.tr(trs, "Tags");
    this.notes = this.tr(trs, "Notes", el("textarea"));
    this.save = el("button.save_btn", "SAVE");
    this.next = el("button.save_btn", "NEXT");
    this.tagsList = new AddedTags();
    this.addTagBtn = el("button.add_tags_btn");
    mount(this.tags, el("span.added_tags_wrap", [this.tagsList, this.addTagBtn]) );

    this.el = el("#content.visible", [
      el('.top', [this.favicon, this.id_info]),
      this.title,
      el("table", [el("tbody", trs)]),
      el(".bottom", [this.next, this.save])
    ]);

    this.status.onchange = () => this.change("status", this.status.value);
    this.notes.onchange = () => this.change("notes", this.notes.value);
    this.tagsList.onRemove = () => this.change("tags", this.tagsList.tags);
    this.addTagBtn.onclick = () => this.onaddTag();
    this.save.onclick = () => this.onsave();
    this.next.onclick = () => stateManger.loadItemByIndex(stateManger.dataIndex + 1).then(item => {
      this.update(item);
      history.replaceState({},item.title,BASE_PATH.concat("?item=" + item.id));
    });
  }

  onsave() {
    const update = {};
    let count = 0;
    this.changes.forEach(t => {
      if (t.isChanged()) {
        update[t.field] = t.getUpdated();
        count++;
      }
    });
    if (count == 0) {
      alert("nothing to update");
      this.changes.clear();
      utils.disable(this.save, true);
    } else {
      update.id = this.loadedItem.id;
      http.POST(BASE_DATA_PATH.concat("item"), update)
        .then(res => {
          if (res.status >= 200 && res.status < 300) {
            this.changes.clear();
            utils.disable(this.save, true);
            stateManger.statusMap.set(update.id, update.status);
          }
        })
        .catch(res => alert("update failed" + res));
    }
  }

  onaddTag() {
    utils.hide(this);
    if (!this.tagsAdder) {
      this.tagsAdder = new TagsAdder();
      mount(container, this.tagsAdder);
    }
    utils.unhide(this.tagsAdder);
    this.tagsAdder.update(this.loadedItem.title, [...this.loadedItem.tags]);
    this.tagsAdder.onclose = items => {
      utils.hide(this.tagsAdder);
      utils.unhide(this);
      this.tagsList.set(items);
      this.change("tags", items);
    };
  }

  change(field, value) {
    let change = this.changes.get(field);
    if (!change) {
      change = new ChangeDetector(this.loadedItem, field);
      this.changes.set(field, change);
    }
    change.set(value);
    let changed = false;
    this.changes.forEach(t => {
      changed = changed || t.isChanged();
    });
    utils.disable(this.save, !changed);
  }

  tr(trs, title, item) {
    const td = item ? el("td", [item]) : el("td");
    trs.push(el("tr", [el("td", title), td]));
    return item || td;
  }
  update(json) {
    utils.disable(this.next, !stateManger.page || (!stateManger.dataIndex && stateManger.dataIndex !== 0));
    this.loadedItem = json;
    this.changes = new Map();
    json.tags = json.tags || [];
    json.notes = json.notes || "";

    utils.disable(this.save);
    utils.setAttr(this.favicon, "src", json.favicon || "");
    utils.setText(this.title, json.title);
    utils.setLink(this.source, json.source);
    utils.setLink(this.redirect, json.redirect);
    utils.setText(this.addedOn, json.addedOn);
    utils.setText(this.notes, json.notes);
    this.status.value = json.status;
    this.tagsList.set([...json.tags]);
    this.id_info.innerText = `${stateManger.dataIndex || ''} / ${json.id}`;
  }
}

class Tag {
  constructor() {
    this.el = el("li");
  }
  update(item) {
    this.el.textContent = item.name;
    if (item.hidden) {
      this.el.classList.add("hidden");
    } else {
      this.el.classList.remove("hidden");
    }
  }
}

let itemList, contentPane;

const update = () => {
  const url = new URL(location.href);
  utils.hide(itemList);
  utils.hide(contentPane);

  if (url.searchParams.get("item")) {
    if (!contentPane) {
      contentPane = new ContentPane();
      mount(container, contentPane);
    }
    utils.unhide(contentPane);
    stateManger.loadItem(Number(url.searchParams.get("item"))).then(data => contentPane.update(data));
  } else {
    if (!itemList) {
      itemList = new ItemList();
      mount(container, itemList);
    }
    utils.unhide(itemList);
    if(stateManger.page) {
      itemList.update(stateManger.page.data);
    } else {
      stateManger.loadPage(0).then(page => itemList.update(page.data));
    } 
  }
};

stateManger.metas()
  .then(() => {
    window.onpopstate = history.onpushstate = update;
    update();
  })
  .catch(err => {
    alert(err.toString());
    console.error(err);
  });
