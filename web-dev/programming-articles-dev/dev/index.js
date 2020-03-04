const $ = s => document.querySelector(s);
const $id = s => document.getElementById(s);
const loading_indicator = $id("loading_indicator");
const container = document.getElementById("container");
let metas;

const { el, mount, list } = require("redom");
const clone = require("lodash/clone");
const isEqual = require("lodash/isEqual");
window.BASE_PATH = window.BASE_PATH || "/programming-articles";

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
    if(el)
    el.el.classList.add("hidden");
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
      if (child.tagName === tagName) {
        return child;
      }
      child = child.parentElement;
    }
  }
};

Object.freeze(utils);

const httpGet = async (url, contentType = "application/json;charset=UTF-8") => {
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
};

const httpPost = async (
  url,
  body,
  contentType = "application/json;charset=UTF-8"
) => {
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

    this.next_btn.onclick = () => loadList(this.pageNum + 1);
    this.prev_btn.onclick = () => loadList(this.pageNum - 1);

    this.el = el("section#items", [
      el(".top", [this.prev_btn, this.page_info, this.next_btn]),
      this.dataItemList
    ]);
    this.dataItemList.el.onclick = e => {
      const target = utils.nearestParent(e.target, "LI");
      if (target.dataset.itemid) {
        history.pushState(
          {},
          "",
          BASE_PATH.concat("?item=" + target.dataset.itemid)
        );
        this.initUpdate();
      }
    };
  }
  update(pageNum) {
    if (pageNum === this.pageNum) {
      return;
    }

    httpGet(BASE_PATH.concat("/page/" + pageNum))
      .then(res => res.json())
      .then(json => {
        this.data = json;
        this.dataItemList.update(json.data);
        this.pageNum = pageNum;
        utils.disable(this.prev_btn, pageNum === 0);
        this.updateCount();
      });
  }
  updateCount() {
    const list = this.dataItemList.el.children;
    let text = `page: ${this.data.page}, count: ${list.length}`;
    if (list.length > 1) {
      text += `, range: ${list[0].dataset.itemid} - ${list[list.length - 1].dataset.itemid}`;
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
        if (this.onRemove) {
          this.onRemove(s);
        }
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

    this.searchBox.onkeyup = e => {
      if (e.keyCode === 13) {
        if (this.searchBox.value && this.searchBox.value.trim())
          this.addTag(this.searchBox.value.trim());
        this.searchBox.value = "";
      } else {
        const text = this.searchBox.value.toLowerCase();
        this.tagsList.update(
          !text
            ? this.loadedTags
            : this.loadedTags.filter(s => s.lowercased.includes(text))
        );
      }
    };

    this.tagsList.el.onclick = e => {
      if (e.target.tagName === "LI") {
        this.addTag(e.target.innerText);
      }
    };
  }

  addTag(tag) {
    this.tagsList.update(this.loadedTags);
    this.addedTags.add(tag);
  }

  update(title, addedTagsItems) {
    this.title.textContent = title;
    this.addedTagsItems = addedTagsItems;
    if (!this.loadedTags) {
      httpGet(BASE_PATH.concat("/tags"), "text/plain")
        .then(res => res.text())
        .then(text => {
          this.loadedTags = Array.from(text.split(/\r?\n/), s => ({
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
    this.title = el("h1.title");
    const trs = [];
    this.source = this.tr(trs, "Source", el("a", { target: "_blank" }));
    this.redirect = this.tr(trs, "Redirect", el("a", { target: "_blank" }));
    this.addedOn = this.tr(trs, "Added On");
    this.status = this.tr(
      trs,
      "Status",
      el(
        "select.status",
        metas.allStatus.map(s =>
          el("option", { value: s, textContent: s })
        )
      )
    );
    this.tags = this.tr(trs, "Tags");
    this.notes = this.tr(trs, "Notes", el("textarea"));
    this.save = el("button.save_btn", "SAVE");
    this.tagsList = new AddedTags();
    this.addTagBtn = el("button.add_tags_btn");
    mount(
      this.tags,
      el("span.added_tags_wrap", [this.tagsList, this.addTagBtn])
    );

    this.el = el("#content.visible", [
      this.favicon,
      this.title,
      el("table", [el("tbody", trs)]),
      el(".bottom", [this.save])
    ]);

    this.status.onchange = () => this.change("status", this.status.value);
    this.notes.onchange = () => this.change("notes", this.notes.value);
    this.tagsList.onRemove = () => this.change("tags", this.tagsList.tags);
    this.addTagBtn.onclick = () => {
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
    };
    this.save.onclick = () => {
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
        httpPost(BASE_PATH.concat("/item_update"), update)
          .then(res => {
            if (res.status >= 200 && res.status < 300) {
              this.changes.clear();
              utils.disable(this.save, true);
            }
          })
          .catch(res => alert("update failed" + res));
      }
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
  update(id) {
    httpGet(BASE_PATH.concat("/item/" + id))
      .then(res => res.json())
      .then(json => {
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
      });
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
    if(!contentPane) {
      contentPane = new ContentPane();
      mount(container, contentPane);
    }
    utils.unhide(contentPane);
    contentPane.update(Number(url.searchParams.get("item")));
  } else {
    if(!itemList) {
      itemList = new ItemList();
      mount(container, itemList);
      itemList.initUpdate = update;
    }
    utils.unhide(itemList);
    itemList.update(0);
    if (
      itemList.data &&
      contentPane.loadedItem &&
      contentPane.loadedItem.status != itemList.data.status
    ) {
      const d = document.getElementById("list-item-" + contentPane.loadedItem.id);
      if (d) {
        d.remove();
        itemList.updateCount();
      }
    }
  }
};

httpGet(BASE_PATH.concat("/metas"))
  .then(res => res.json())
  .then(json => {
    metas = json;
    window.onpopstate = history.onpushstate = update;
    update();
  })
  .catch(err => alert(err.toString()));
